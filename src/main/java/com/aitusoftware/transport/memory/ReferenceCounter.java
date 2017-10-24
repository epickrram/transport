package com.aitusoftware.transport.memory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReferenceCounter
{
    private static final int UNREACHABLE = Integer.MIN_VALUE;
    private final AtomicInteger referenceCount = new AtomicInteger();
    private long lastClaimedNanos = 0L;

    public boolean claim()
    {
        final boolean claimed = referenceCount.incrementAndGet() > 0;

        if (!claimed)
        {
            referenceCount.decrementAndGet();
        }
        if (claimed)
        {
            lastClaimedNanos = System.nanoTime();
        }
        return claimed;
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
}