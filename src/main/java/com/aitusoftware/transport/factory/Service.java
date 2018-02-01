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
    private final ExecutorService executor =
            newCachedThreadPool(daemonFactory());

    Service(final StreamingReader inboundReader,
            final Collection<Named<StreamingReader>> readers,
            final Server server)
    {
        this.inboundReader = inboundReader;
        this.readers = readers;
        this.server = server;
    }

    public void start()
    {
        readers.forEach(reader -> {
            executor.submit(loggingRunnable(namedThread(reader.name(),
                    reader.value()::process)));
        });
        executor.submit(loggingRunnable(namedThread("inbound-message-dispatcher", inboundReader::process)));
        server.start(executor);

        server.waitForStartup(5, TimeUnit.SECONDS);
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