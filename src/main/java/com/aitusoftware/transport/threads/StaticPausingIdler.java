package com.aitusoftware.transport.threads;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

final class StaticPausingIdler implements Idler
{
    private final TimeUnit timeUnit;
    private final long duration;

    StaticPausingIdler(final long duration, final TimeUnit timeUnit)
    {
        this.timeUnit = timeUnit;
        this.duration = duration;
    }

    @Override
    public void idle()
    {
        LockSupport.parkNanos(timeUnit.toNanos(duration));
    }

    @Override
    public void reset()
    {
        // no-op
    }
}