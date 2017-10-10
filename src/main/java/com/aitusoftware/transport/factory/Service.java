package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.net.Server;
import com.aitusoftware.transport.reader.StreamingReader;
import org.agrona.collections.Int2ObjectHashMap;

import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.aitusoftware.transport.threads.Threads.daemonFactory;
import static com.aitusoftware.transport.threads.Threads.namedThread;
import static java.util.concurrent.Executors.newFixedThreadPool;

public final class Service
{
    private final StreamingReader inboundReader;
    private final StreamingReader outboundReader;
    private final Server server;
    private final ExecutorService executor =
            newFixedThreadPool(3, daemonFactory());

    Service(final StreamingReader inboundReader, final StreamingReader outboundReader,
            final Server server)
    {
        this.inboundReader = inboundReader;
        this.outboundReader = outboundReader;
        this.server = server;
    }

    public void start()
    {
        executor.submit(namedThread("outbound-message-processor", outboundReader::process));
        executor.submit(namedThread("inbound-message-dispatcher", inboundReader::process));
        executor.submit(namedThread("request-server", server::start));

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