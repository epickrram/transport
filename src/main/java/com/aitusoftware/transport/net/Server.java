/*
 * Copyright 2017 - 2018 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aitusoftware.transport.net;

import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.buffer.WritableRecord;
import com.aitusoftware.transport.factory.SubscriberThreading;
import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.IntHashSet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static com.aitusoftware.transport.threads.Threads.loggingRunnable;
import static com.aitusoftware.transport.threads.Threads.namedThread;

public final class Server
{
    private final IntHashSet subscriberTopicIds;
    private final IntFunction<ServerSocketChannel> socketFactory;
    private final PageCache subscriberPageCache;
    private final ServerTopicChannel[] serverSocketChannels;
    private final SubscriberThreading subscriberThreading;
    private final Int2ObjectHashMap<Class<?>> topicIdToTopic;
    private final List<TopicChannel> channels = new ArrayList<>();
    private final Idler idler = Idlers.staticPause(1, TimeUnit.MILLISECONDS);
    private final CountDownLatch listenerStarted = new CountDownLatch(1);

    public Server(final IntHashSet subscriberTopicIds,
                  final IntFunction<ServerSocketChannel> socketFactory,
                  final PageCache subscriberPageCache, final SubscriberThreading subscriberThreading,
                  final Int2ObjectHashMap<Class<?>> topicIdToTopic)
    {
        this.subscriberTopicIds = subscriberTopicIds;
        this.socketFactory = socketFactory;
        this.subscriberPageCache = subscriberPageCache;
        serverSocketChannels = new ServerTopicChannel[subscriberTopicIds.size()];
        this.subscriberThreading = subscriberThreading;
        this.topicIdToTopic = topicIdToTopic;
    }

    public void start(final ExecutorService executor)
    {
        switch (subscriberThreading)
        {
            case SINGLE_THREADED:
                final int[] topicIds = new int[subscriberTopicIds.size()];
                int ptr = 0;
                for (Integer topicId : subscriberTopicIds)
                {
                    topicIds[ptr++] = topicId;
                }
                executor.submit(loggingRunnable(namedThread("request-server",
                        singleThreadReceiveLoop(topicIds))));
                break;
            case THREAD_PER_TOPIC:
                for (Integer subscriberTopicId : subscriberTopicIds)
                {
                    final Class<?> topic = topicIdToTopic.get(subscriberTopicId);
                    executor.submit(loggingRunnable(namedThread("request-server-" + topic.getSimpleName(),
                            singleThreadReceiveLoop(new int[] {subscriberTopicId}))));
                }
                break;
            case THREAD_PER_CONNECTION:
                throw new UnsupportedOperationException();
            default:
                throw new IllegalArgumentException();
        }
    }

    public void waitForStartup(final long duration, final TimeUnit unit)
    {
        try
        {
            if (!listenerStarted.await(duration, unit))
            {
                throw new IllegalStateException("Server sockets not started");
            }
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException("Interrupted while waiting for startup", e);
        }
    }

    private Runnable singleThreadReceiveLoop(final int[] subscriberTopicIds)
    {
        // TODO clean up, optimise for single topic per thread (remove loops)
        return () ->
        {
            int ptr = 0;
            for (final int topicId : subscriberTopicIds)
            {
                final ServerSocketChannel channel = socketFactory.apply(topicId);
                serverSocketChannels[ptr] = new ServerTopicChannel(channel);

                ptr++;
            }

            listenerStarted.countDown();
            while (!Thread.currentThread().isInterrupted())
            {
                boolean dataProcessed = acceptedNewConnections(subscriberTopicIds.length);

                for (int i = 0; i < channels.size(); i++)
                {
                    final TopicChannel topicChannel = channels.get(i);
                    try
                    {
                        topicChannel.readLength();
                        if (topicChannel.isReady())
                        {
                            dataProcessed = true;
                            final int length = topicChannel.getLength();
                            final WritableRecord record = subscriberPageCache.acquireRecordBuffer(length);
                            try
                            {
                                final ByteBuffer buffer = record.buffer();
                                do
                                {
                                    topicChannel.channel.read(buffer);
                                }
                                while (buffer.remaining() != 0);
                            }
                            finally
                            {
                                record.commit();
                            }
                        }
                    }
                    catch (UncheckedIOException | IOException e)
                    {
                        channels.remove(topicChannel);
                    }
                }

                if (!dataProcessed)
                {
                    idler.idle();
                }
                else
                {
                    idler.reset();
                }
            }
        };
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private boolean acceptedNewConnections(final int serverSocketCount)
    {
        boolean connectionAccepted = false;
        for (int i = 0; i < serverSocketCount; i++)
        {
            try
            {
                final SocketChannel accepted = serverSocketChannels[i].channel.accept();
                if (accepted != null)
                {
                    accepted.configureBlocking(false);
                    channels.add(new TopicChannel(accepted));
                    connectionAccepted = true;
                }
            }
            catch (NotYetBoundException e)
            {
                // ignore, server socket is not ready yet
            }
            catch (IOException e)
            {
                e.printStackTrace();
                // TODO emit event
            }
        }
        return connectionAccepted;
    }

    private static final class TopicChannel
    {
        private final SocketChannel channel;
        private final ByteBuffer lengthBuffer = ByteBuffer.allocateDirect(4);

        TopicChannel(final SocketChannel channel)
        {
            this.channel = channel;
        }

        boolean isReady()
        {
            return lengthBuffer.position() == 4;
        }

        int getLength()
        {
            final int length = lengthBuffer.flip().getInt();
            lengthBuffer.clear();
            return length;
        }

        void readLength()
        {
            try
            {
                if (-1 == channel.read(lengthBuffer))
                {
                    throw new IOException("End of stream");
                }
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static final class ServerTopicChannel
    {
        private final ServerSocketChannel channel;

        ServerTopicChannel(final ServerSocketChannel channel)
        {
            this.channel = channel;
        }
    }
}