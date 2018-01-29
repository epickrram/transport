package com.aitusoftware.transport.net;

import org.agrona.collections.Int2ObjectHashMap;

import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SocketChannel;
import java.util.function.IntFunction;

public final class TopicToChannelMapper
{
    private final IntFunction<SocketChannel> connector;
    private final Int2ObjectHashMap<SocketChannel> openChannels =
            new Int2ObjectHashMap<>();

    public TopicToChannelMapper(final IntFunction<SocketChannel> connector)
    {
        this.connector = connector;
    }

    GatheringByteChannel forTopic(final int topicId)
    {
        SocketChannel channel = openChannels.get(topicId);
        if (channel != null)
        {
            return channel;
        }
        final SocketChannel connected = connector.apply(topicId);
        openChannels.put(topicId, connected);
        return connected;
    }

    void reconnectChannel(final int topicId)
    {
        openChannels.remove(topicId);
    }
}
