package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.transport.buffer.Fixtures;
import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.messaging.TestTopic;
import com.aitusoftware.transport.reader.RecordHandler;
import com.aitusoftware.transport.reader.StreamingReader;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class ProxyIntegrationTest
{
    private final Path tempDir = Fixtures.tempDirectory();
    private PageCache pageCache;
    private PublisherFactory factory;
    private final SubscriberFactory subscriberFactory = new SubscriberFactory();

    @Before
    public void setUp() throws Exception
    {
        pageCache = PageCache.create(tempDir, 256);
        factory = new PublisherFactory(pageCache);
    }

    @Test
    public void shouldLoadPublisher() throws Exception
    {
        final TestTopic proxy = factory.getPublisherProxy(TestTopic.class);
        proxy.say("hola", 7);
        proxy.say("bonjour", 11);

        assertThat(pageCache.estimateTotalLength(), is(128L));
    }

    @Test
    public void shouldLoadSubscriber() throws Exception
    {
        final Subscriber subscriber =
                subscriberFactory.getSubscriber(TestTopic.class, (message, counter) -> {

        });
    }

    @Test
    public void shouldSendMessagesViaPageCache() throws Exception
    {
        final TestTopic proxy = factory.getPublisherProxy(TestTopic.class);

        final Capture capture = new Capture();
        final Subscriber<TestTopic> subscriber =
                subscriberFactory.getSubscriber(TestTopic.class, capture);

        proxy.say("hola", 7);
        proxy.say("bonjour", 11);

        new StreamingReader(pageCache, new RecordHandler()
        {
            @Override
            public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
            {
                data.getInt();
                subscriber.onRecord(data, pageNumber, position);
            }
        }, false).process();

        assertThat(capture.received.size(), is(2));
        assertThat(capture.received.get("hola"), is(7));
        assertThat(capture.received.get("bonjour"), is(11));
    }

    private static class Capture implements TestTopic
    {
        private final Map<String, Integer> received = new HashMap<>();

        @Override
        public void say(final CharSequence message, final int counter)
        {
            received.put(message.toString(), counter);
        }
    }
}
