/*
 * Copyright 2017 - 2018 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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