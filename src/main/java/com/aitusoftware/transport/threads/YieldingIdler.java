package com.aitusoftware.transport.threads;

final class YieldingIdler implements Idler
{
    @Override
    public void idle()
    {
        Thread.yield();
    }

    @Override
    public void reset()
    {
        // no-op
    }
}
