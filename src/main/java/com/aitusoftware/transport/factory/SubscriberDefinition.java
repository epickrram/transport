package com.aitusoftware.transport.factory;

import java.net.SocketAddress;

public final class SubscriberDefinition<T>
{
    private final Class<T> topic;
    private final T implementation;
    private final SocketAddress socketAddress;

    public SubscriberDefinition(final Class<T> topic, final T implementation,
                                final SocketAddress socketAddress)
    {
        this.topic = topic;
        this.implementation = implementation;
        this.socketAddress = socketAddress;
    }

    Class<T> getTopic()
    {
        return topic;
    }

    T getImplementation()
    {
        return implementation;
    }

    SocketAddress getSocketAddress()
    {
        return socketAddress;
    }
}
