package com.aitusoftware.transport.threads;

import java.util.concurrent.TimeUnit;

public final class Idlers
{
    private Idlers()
    {
    }

    public static Idler busy()
    {
        return new BusySpinIdler();
    }

    public static Idler yielding()
    {
        return new YieldingIdler();
    }

    public static Idler adaptive(final long maxPause, final TimeUnit pauseUnit)
    {
        return new AdaptiveIdler(maxPause, pauseUnit);
    }

    public static Idler staticPause(final long pause, final TimeUnit pauseUnit)
    {
        return new StaticPausingIdler(pause, pauseUnit);
    }
}
