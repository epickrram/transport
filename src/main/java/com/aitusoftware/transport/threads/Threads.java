package com.aitusoftware.transport.threads;

import com.aitusoftware.transport.ffi.Affinity;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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

    public static Runnable withAffinity(
            final Runnable delegate, final int cpu, final Consumer<Throwable> failureHandler)
    {
        return () -> {
            final Affinity affinity = new Affinity();
            try
            {
                affinity.setCurrentThreadCpuAffinity(cpu);
                final int cpuAffinity = affinity.getCurrentThreadCpuAffinity();
                if (cpu != cpuAffinity)
                {
                    throw new IllegalStateException(
                            String.format("Unable to set cpu affinity, expected %d but was %d", cpu, cpuAffinity));
                }
            }
            catch (Throwable t)
            {
                failureHandler.accept(t);
            }
            delegate.run();
        };
    }
}