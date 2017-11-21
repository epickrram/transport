package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public final class Unmapper
{
    private final Queue<Page> activePages = new ConcurrentLinkedQueue<>();
    private final Idler idler = Idlers.staticPause(1, TimeUnit.MILLISECONDS);

    void registerPage(final Page page)
    {
        activePages.add(page);
    }

    public void execute()
    {
        while (!Thread.currentThread().isInterrupted())
        {
            boolean idle = true;
            final Set<Page> forRemoval = new HashSet<>();
            for (Page page : activePages)
            {
                if (page.acquireForCleanup())
                {
                    idle = false;
                    page.unmap();
                    forRemoval.add(page);
                }
            }
            activePages.removeAll(forRemoval);

            if (idle)
            {
                idler.idle();
            }
        }
    }
}
