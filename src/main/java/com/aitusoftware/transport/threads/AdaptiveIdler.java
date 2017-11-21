package com.aitusoftware.transport.threads;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

final class AdaptiveIdler implements Idler
{
    private static final int INITIAL_COUNT = 200;
    private final long maxPause;
    private final TimeUnit pauseUnit;
    private int spinCount = INITIAL_COUNT;
    private int yieldCount = INITIAL_COUNT;
    private long nextPause;

    AdaptiveIdler(final long maxPause, final TimeUnit pauseUnit)
    {
        this.maxPause = maxPause;
        this.pauseUnit = pauseUnit;
    }

    @Override
    public void idle()
    {
        if (spinCount != 0)
        {
            spinCount--;
            return;
        }

        if (yieldCount != 0)
        {
            yieldCount--;
            return;
        }

        LockSupport.parkNanos(nextPause);
        nextPause = Math.min(nextPause * 2, pauseUnit.toNanos(maxPause));
    }

    @Override
    public void reset()
    {
        spinCount = INITIAL_COUNT;
        yieldCount = INITIAL_COUNT;
        nextPause = 1L;
    }
}
