package com.aitusoftware.transport.memory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReferenceCounter
{
    private static final int UNREACHABLE = Integer.MIN_VALUE;
    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private long lastClaimedNanos = 0L;

    public boolean claim()
    {
        while (true)
        {
            final int current = referenceCount.get();
            if (current == UNREACHABLE)
            {
                return false;
            }

            if (referenceCount.compareAndSet(current, current + 1))
            {
                lastClaimedNanos = System.nanoTime();
                return true;
            }
        }
    }

    public void release()
    {
        final int countBeforeRelease = referenceCount.getAndDecrement();
        if (countBeforeRelease < 1)
        {
            if (countBeforeRelease == UNREACHABLE)
            {
                throw new IllegalStateException("Reference already unreachable");
            }
            throw new IllegalStateException("Reference Count < 0");
        }
    }

    public int getReferenceCount()
    {
        return referenceCount.get();
    }

    public boolean makeUnreachable()
    {
        return referenceCount.get() == 0 &&
                referenceCount.compareAndSet(0, UNREACHABLE);

    }

    public boolean lastClaimIsOlderThan(final int duration, final TimeUnit unit)
    {
        return lastClaimedNanos + unit.toNanos(duration) < System.nanoTime();
    }

    @Override
    public String toString()
    {
        return "ReferenceCounter{" +
                "referenceCount=" + referenceCount +
                ", lastClaimedNanos=" + lastClaimedNanos +
                '}';
    }
}