package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.net.ServerSocketFactory;

import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;

final class FixedServerSocketFactory implements ServerSocketFactory
{
    private final ServerSocketChannel channel;

    FixedServerSocketFactory(final ServerSocketChannel channel)
    {
        this.channel = channel;
    }

    @Override
    public void registerTopicAddress(final int topicId, final SocketAddress socketAddress)
    {
        // no-op
    }

    @Override
    public ServerSocketChannel acquire(final int topicId)
    {
        return channel;
    }
}
