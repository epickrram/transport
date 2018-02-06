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
package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.net.Server;
import com.aitusoftware.transport.reader.StreamingReader;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.aitusoftware.transport.threads.Threads.daemonFactory;
import static com.aitusoftware.transport.threads.Threads.loggingRunnable;
import static com.aitusoftware.transport.threads.Threads.namedThread;
import static java.util.concurrent.Executors.newCachedThreadPool;

public final class Service
{
    private final StreamingReader inboundReader;
    private final Collection<Named<StreamingReader>> readers;
    private final Server server;
    private final boolean hasRemoteSubscribers;
    private final ExecutorService executor =
            newCachedThreadPool(daemonFactory());

    Service(final StreamingReader inboundReader,
            final Collection<Named<StreamingReader>> readers,
            final Server server, final boolean hasRemoteSubscribers)
    {
        this.inboundReader = inboundReader;
        this.readers = readers;
        this.server = server;
        this.hasRemoteSubscribers = hasRemoteSubscribers;
    }

    public void start()
    {
        readers.forEach(reader -> {
            executor.submit(loggingRunnable(namedThread(reader.name(),
                    reader.value()::process)));
        });
        executor.submit(loggingRunnable(namedThread("inbound-message-dispatcher", inboundReader::process)));
        if (hasRemoteSubscribers)
        {
            server.start(executor);
            server.waitForStartup(5, TimeUnit.SECONDS);
        }
    }

    public boolean stop(final long timeout, final TimeUnit timeUnit)
    {
        executor.shutdownNow();
        try
        {
            return executor.awaitTermination(timeout, timeUnit);
        }
        catch (InterruptedException e)
        {
            // ignore
        }
        return false;
    }
}