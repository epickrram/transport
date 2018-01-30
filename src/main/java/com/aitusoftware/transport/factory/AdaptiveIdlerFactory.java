package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class AdaptiveIdlerFactory
{
    private final long maxPause;
    private final TimeUnit pauseUnit;

    private AdaptiveIdlerFactory(final long maxPause, final TimeUnit pauseUnit)
    {
        this.maxPause = maxPause;
        this.pauseUnit = pauseUnit;
    }

    private Idler forPublisher(final Class<?> topicClass)
    {
        return Idlers.adaptive(maxPause, pauseUnit);
    }

    public static Function<Class<?>, Idler> idleUpTo(final long maxPause, final TimeUnit pauseUnit)
    {
        return new AdaptiveIdlerFactory(maxPause, pauseUnit)::forPublisher;
    }
}
