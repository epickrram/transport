package com.aitusoftware.transport.net;

import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.buffer.WritableRecord;
import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.PausingIdler;
import org.agrona.collections.IntHashSet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

public final class Server
{
    private final IntHashSet subscriberTopicIds;
    private final IntFunction<ServerSocketChannel> socketFactory;
    private final PageCache subscriberPageCache;
    private final ServerTopicChannel[] serverSocketChannels;
    // TODO reduce garbage
    private final List<TopicChannel> channels = new LinkedList<>();
    private final Idler idler = new PausingIdler(1, TimeUnit.MILLISECONDS);
    private final CountDownLatch listenerStarted = new CountDownLatch(1);

    public Server(final IntHashSet subscriberTopicIds, final IntFunction<ServerSocketChannel> socketFactory,
                  final PageCache subscriberPageCache)
    {
        this.subscriberTopicIds = subscriberTopicIds;
        this.socketFactory = socketFactory;
        this.subscriberPageCache = subscriberPageCache;
        serverSocketChannels = new ServerTopicChannel[subscriberTopicIds.size()];
    }

    public void start()
    {
        int ptr = 0;
        for (final int topicId : subscriberTopicIds)
        {
            final ServerSocketChannel channel = socketFactory.apply(topicId);

            System.out.printf("Listening to topic %d on %s%n", topicId, channel);

            serverSocketChannels[ptr] = new ServerTopicChannel(channel, topicId);
            ptr++;
        }

        listenerStarted.countDown();

        while (!Thread.currentThread().isInterrupted())
        {
            acceptNewConnections();

            boolean dataProcessed = false;

            for (final TopicChannel topicChannel : channels)
            {
                try
                {
                    topicChannel.readLength();
                    if (topicChannel.isReady())
                    {
                        System.out.printf("Received data on %s%n", topicChannel.channel);
                        dataProcessed = true;
                        final WritableRecord record = subscriberPageCache.acquireRecordBuffer(topicChannel.getLength());
                        try
                        {
                            final ByteBuffer buffer = record.buffer();
                            final int position = buffer.position();
                            while (buffer.remaining() != 0)
                            {
                                topicChannel.channel.read(buffer);
                            }

                            System.out.printf("Wrote to topic %d from %s%n",
                                    buffer.getInt(position), topicChannel.channel);
                        }
                        finally
                        {
                            record.commit();
                        }
                    }
                }
                catch (IOException e)
                {
                    channels.remove(topicChannel);
                }
            }

            if (!dataProcessed)
            {
                idler.idle();
            }
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
            throw new RuntimeException("Interrupted while waiting for startup", e);
        }
    }

    private void acceptNewConnections()
    {
        for (int i = 0; i < serverSocketChannels.length; i++)
        {
            try
            {
                final SocketChannel accepted = serverSocketChannels[i].channel.accept();
                if (accepted != null)
                {
                    channels.add(new TopicChannel(accepted));
                    System.out.printf("new connection %s%n", accepted);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                // TODO emit event
            }
        }
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
                channel.read(lengthBuffer);
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
        private final int topicId;

        ServerTopicChannel(final ServerSocketChannel channel, final int topicId)
        {
            this.channel = channel;
            this.topicId = topicId;
        }
    }
}