package com.aitusoftware.transport.net;

import org.agrona.collections.Int2ObjectHashMap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerSocketFactoryImpl implements ServerSocketFactory
{
    private final Int2ObjectHashMap<SocketAddress> topicToListenerAddress =
            new Int2ObjectHashMap<>();
    private final Map<SocketAddress, ServerSocketChannel> createdSockets = new ConcurrentHashMap<>();

    @Override
    public void registerTopicAddress(final int topicId, final SocketAddress socketAddress)
    {
        topicToListenerAddress.put(topicId, socketAddress);
    }

    @Override
    public ServerSocketChannel acquire(final int topicId)
    {
        return createdSockets.computeIfAbsent(topicToListenerAddress.get(topicId), addr -> {
            if (addr == null)
            {
                return null;
            }
            try
            {
                final ServerSocketChannel serverChannel = ServerSocketChannel.open();
                serverChannel.bind(addr);
                serverChannel.configureBlocking(false);
                return serverChannel;
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        });
    }
}
