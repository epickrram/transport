package com.aitusoftware.transport.factory;

public final class SubscriberDefinition<T>
{
    private final Class<T> topic;
    private final T implementation;
    private final Media media;

    public SubscriberDefinition(final Class<T> topic, final T implementation,
                                final Media media)
    {
        this.topic = topic;
        this.implementation = implementation;
        this.media = media;
    }

    Class<T> getTopic()
    {
        return topic;
    }

    T getImplementation()
    {
        return implementation;
    }

    Media getMedia()
    {
        return media;
    }
}
