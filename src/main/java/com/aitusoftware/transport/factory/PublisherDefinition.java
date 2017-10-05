package com.aitusoftware.transport.factory;

import java.util.function.Consumer;

public final class PublisherDefinition<T>
{
    private final Class<T> topic;
    private final Consumer<T> receiver;

    public PublisherDefinition(final Class<T> topic, final Consumer<T> receiver)
    {
        this.topic = topic;
        this.receiver = receiver;
    }

    Class<T> getTopic()
    {
        return topic;
    }

    Consumer<T> getReceiver()
    {
        return receiver;
    }
}