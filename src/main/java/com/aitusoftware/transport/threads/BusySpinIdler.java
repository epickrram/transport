package com.aitusoftware.transport.threads;

final class BusySpinIdler implements Idler
{
    @Override
    public void idle()
    {
        Thread.onSpinWait();
    }

    @Override
    public void reset()
    {
        // no-op
    }
}
