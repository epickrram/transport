package com.aitusoftware.transport.net;

import com.aitusoftware.transport.buffer.PageCache;
import org.agrona.collections.Int2ObjectHashMap;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

public final class Server
{
    private final Int2ObjectHashMap<SocketAddress> listenAddresses;
    private final PageCache subscriberPageCache;
    private final ServerTopicChannel[] serverSocketChannels;
    // TODO reduce garbage
    private final List<TopicChannel> channels = new LinkedList<>();

    public Server(final Int2ObjectHashMap<SocketAddress> listenAddresses,
                  final PageCache subscriberPageCache)
    {
        this.listenAddresses = listenAddresses;
        this.subscriberPageCache = subscriberPageCache;
        serverSocketChannels = new ServerTopicChannel[listenAddresses.size()];
    }

    void start() throws IOException
    {
        int ptr = 0;
        for (final int topicId : listenAddresses.keySet())
        {
            final ServerSocketChannel channel = ServerSocketChannel.open();
            channel.bind(listenAddresses.get(topicId));
            channel.configureBlocking(false);
            serverSocketChannels[ptr] = new ServerTopicChannel(channel, topicId);
            ptr++;
        }
        while (!Thread.currentThread().isInterrupted())
        {
            for (int i = 0; i < serverSocketChannels.length; i++)
            {
                final SocketChannel accepted = serverSocketChannels[i].channel.accept();
                if (accepted != null)
                {
                    channels.add(new TopicChannel(accepted, serverSocketChannels[i].topicId));
                }
            }

            for (final TopicChannel topicChannel : channels)
            {
                // read length, claim buffer
            }
        }
    }

    private static final class TopicChannel
    {
        private final SocketChannel channel;
        private final int topicId;

        TopicChannel(final SocketChannel channel, final int topicId)
        {
            this.channel = channel;
            this.topicId = topicId;
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