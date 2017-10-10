package com.aitusoftware.transport.threads;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class Threads
{
    public static ThreadFactory namedDaemonFactory(final String prefix)
    {
        final AtomicInteger counter = new AtomicInteger();
        return r -> {
            final Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(prefix + "-" + counter.getAndIncrement());
            return t;
        };
    }

    public static ThreadFactory daemonFactory()
    {
        return r -> {
            final Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        };
    }

    public static Runnable namedThread(final String name, final Runnable delegate)
    {
        return () -> {
            Thread.currentThread().setName(name);
            delegate.run();
        };
    }

    public static Runnable loggingRunnable(final Runnable delegate)
    {
        return () -> {
            try
            {
                delegate.run();
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        };
    }
}
