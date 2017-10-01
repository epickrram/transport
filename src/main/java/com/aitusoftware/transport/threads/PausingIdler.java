package com.aitusoftware.transport.threads;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.atomic.AtomicIntegerFieldUpdater.newUpdater;
import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

public final class PausingIdler implements Idler
{
    private static final AtomicReferenceFieldUpdater<PausingIdler, Thread> PARKED_THREAD =
            newUpdater(PausingIdler.class, Thread.class, "parkedThread");
    private static final AtomicIntegerFieldUpdater<PausingIdler> WOKEN =
            newUpdater(PausingIdler.class, "woken");

    private final TimeUnit timeUnit;
    private final long duration;
    private volatile Thread parkedThread;
    private volatile int woken;

    public PausingIdler(final long duration, final TimeUnit timeUnit)
    {
        this.timeUnit = timeUnit;
        this.duration = duration;
    }

    @Override
    public void idle()
    {
        WOKEN.set(this, 0);
        PARKED_THREAD.lazySet(this, Thread.currentThread());
        LockSupport.parkNanos(timeUnit.toNanos(duration));

        if (Thread.currentThread().isInterrupted() && WOKEN.get(this) == 0)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void wakeup()
    {
        final Thread thread = PARKED_THREAD.get(this);
        if (thread != null)
        {
            WOKEN.set(this, 1);
            thread.interrupt();
        }
    }
}