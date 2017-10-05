package com.aitusoftware.transport.factory;

public final class SubscriberDefinition<T>
{
    private final Class<T> topic;
    private final T implementation;

    public SubscriberDefinition(final Class<T> topic, final T implementation)
    {
        this.topic = topic;
        this.implementation = implementation;
    }

    Class<T> getTopic()
    {
        return topic;
    }

    T getImplementation()
    {
        return implementation;
    }
}
