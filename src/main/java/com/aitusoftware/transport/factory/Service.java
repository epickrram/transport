package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.reader.StreamingReader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.aitusoftware.transport.threads.Threads.daemonFactory;
import static com.aitusoftware.transport.threads.Threads.namedThread;
import static java.util.concurrent.Executors.newFixedThreadPool;

public final class Service
{
    private final StreamingReader inboundReader;
    private final StreamingReader outboundReader;
    private final ExecutorService executor =
            newFixedThreadPool(2, daemonFactory());

    Service(final StreamingReader inboundReader, final StreamingReader outboundReader)
    {
        this.inboundReader = inboundReader;
        this.outboundReader = outboundReader;
    }

    public void start()
    {
        executor.submit(namedThread("outbound-message-processor", outboundReader::process));
        executor.submit(namedThread("inbound-message-dispatcher", inboundReader::process));
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