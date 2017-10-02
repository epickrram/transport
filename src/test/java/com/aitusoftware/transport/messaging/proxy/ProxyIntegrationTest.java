package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.transport.buffer.Fixtures;
import com.aitusoftware.transport.buffer.PageCache;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Ignore
public final class ProxyIntegrationTest
{
    private final Path tempDir = Fixtures.tempDirectory();
    private final PageCache pageCache = PageCache.create(tempDir, 256);
    private final PublisherFactory factory = new PublisherFactory(pageCache);

    @Test
    public void shouldLoadPublisher() throws Exception
    {
        final TestTopic proxy = factory.getPublisherProxy(TestTopic.class);
        proxy.say("hola", 7);
        proxy.say("bonjour", 11);

        assertThat(pageCache.estimateTotalLength(), is(50));
    }
}
