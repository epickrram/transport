package com.aitusoftware.transport.messaging;

import com.aitusoftware.transport.buffer.Fixtures;
import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.messaging.proxy.PublisherFactory;
import com.aitusoftware.transport.messaging.proxy.Subscriber;
import com.aitusoftware.transport.messaging.proxy.SubscriberFactory;
import com.aitusoftware.transport.reader.StreamingReader;
import org.agrona.collections.Int2ObjectHashMap;
import org.junit.Test;

import java.nio.file.Path;

public final class TopicDispatcherRecordHandlerTest
{
    private final Path tempDir = Fixtures.tempDirectory();
    private final PageCache pageCache = PageCache.create(tempDir, 256);
    private final PublisherFactory factory = new PublisherFactory(pageCache);
    private final SubscriberFactory subscriberFactory = new SubscriberFactory();

    @Test
    public void shouldDispatchMessages() throws Exception
    {
        final Subscriber testTopicSubscriber =
                subscriberFactory.getSubscriber(TestTopic.class, new TestTopic()
                {
                    @Override
                    public void say(final CharSequence message, final int counter)
                    {
                        System.out.println("say " + message);
                    }
                });
        final Subscriber otherTopicSubscriber = subscriberFactory.getSubscriber(OtherTopic.class, new OtherTopic()
        {
            @Override
            public void testParams(final boolean truth, final byte tByte, final short tShort,
                                   final int tInt, final float tFloat, final long tLong,
                                   final double tDouble, final CharSequence zeroCopy, final CharSequence heapCopy)
            {
                System.out.println("testParams");
            }
        });


        final TestTopic proxy = factory.getPublisherProxy(TestTopic.class);
        final OtherTopic paramTester = factory.getPublisherProxy(OtherTopic.class);

        proxy.say("hola", 7);

        paramTester.testParams(true, (byte) 5, (short) 7, 11,
                13.7f, 17L, 19.37d, "first", "second");

        proxy.say("bonjour", 11);

        paramTester.testParams(false, (byte) -5, (short) -7, -11,
                Float.MAX_VALUE, Long.MIN_VALUE, Double.POSITIVE_INFINITY, "first", "second");

        final Int2ObjectHashMap<Subscriber> subscriberMap =
                new Int2ObjectHashMap<>();

        subscriberMap.put(testTopicSubscriber.getTopicId(), testTopicSubscriber);
        subscriberMap.put(otherTopicSubscriber.getTopicId(), otherTopicSubscriber);

        final TopicDispatcherRecordHandler topicDispatcher =
                new TopicDispatcherRecordHandler(subscriberMap);

        new StreamingReader(pageCache, topicDispatcher, false, true).process();


    }

}