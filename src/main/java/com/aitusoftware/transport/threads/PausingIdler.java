package com.aitusoftware.transport.threads;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class PausingIdler implements Idler
{
    private final TimeUnit timeUnit;
    private final long duration;

    public PausingIdler(final long duration, final TimeUnit timeUnit)
    {
        this.timeUnit = timeUnit;
        this.duration = duration;
    }

    @Override
    public void idle()
    {
        LockSupport.parkNanos(timeUnit.toNanos(duration));

        if (Thread.currentThread().isInterrupted())
        {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void wakeup()
    {
        // no-op
    }
}