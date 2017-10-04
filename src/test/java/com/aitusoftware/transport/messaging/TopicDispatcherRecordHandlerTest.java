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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public final class TopicDispatcherRecordHandlerTest
{
    private final Path tempDir = Fixtures.tempDirectory();
    private final PageCache pageCache = PageCache.create(tempDir, 4096);
    private final PublisherFactory factory = new PublisherFactory(pageCache);
    private final SubscriberFactory subscriberFactory = new SubscriberFactory();

    @Test
    public void shouldDispatchMessages() throws Exception
    {
        final MessageCounter messageCounter = new MessageCounter();
        final Subscriber testTopicSubscriber =
                subscriberFactory.getSubscriber(TestTopic.class, messageCounter);
        final Subscriber otherTopicSubscriber = subscriberFactory.getSubscriber(OtherTopic.class, new ParamValidator());

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

        assertThat(messageCounter.messageCount, is(2));
    }

    private static class ParamValidator implements OtherTopic
    {
        @Override
        public void testParams(final boolean truth, final byte tByte, final short tShort,
                               final int tInt, final float tFloat, final long tLong,
                               final double tDouble, final CharSequence zeroCopy, final CharSequence heapCopy)
        {
            if (truth)
            {
                assertThat(tByte, is((byte) 5));
                assertThat(tShort, is((short) 7));
                assertThat(tInt, is(11));
                assertThat(tFloat, is(13.7f));
                assertThat(tLong, is(17L));
                assertThat(tDouble, is(19.37d));
                assertThat(zeroCopy.toString(), is("first"));
                assertThat(heapCopy.toString(), is("second"));
            }
            else
            {
                assertThat(tByte, is((byte) -5));
                assertThat(tShort, is((short) -7));
                assertThat(tInt, is(-11));
                assertThat(tFloat, is(Float.MAX_VALUE));
                assertThat(tLong, is(Long.MIN_VALUE));
                assertThat(tDouble, is(Double.POSITIVE_INFINITY));
                assertThat(zeroCopy.toString(), is("first"));
                assertThat(heapCopy.toString(), is("second"));
            }
        }
    }

    private static class MessageCounter implements TestTopic
    {
        private int messageCount;

        @Override
        public void say(final CharSequence message, final int counter)
        {
            messageCount++;
        }
    }
}