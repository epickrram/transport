package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;

import java.util.concurrent.TimeUnit;

public final class PublisherIdlerFactory
{
    Idler forPublisher(final Class<?> topicClass)
    {
        return Idlers.adaptive(1L, TimeUnit.MILLISECONDS);
    }
}
