package com.aitusoftware.transport.messaging;

import com.aitusoftware.transport.buffer.Fixtures;
import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.messaging.proxy.PublisherFactory;
import com.aitusoftware.transport.messaging.proxy.Subscriber;
import com.aitusoftware.transport.messaging.proxy.SubscriberFactory;
import com.aitusoftware.transport.reader.StreamingReader;
import org.agrona.collections.Int2ObjectHashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Ignore
public final class MultiThreadedTopicDispatcherTest
{
    private static final int NUMBER_OF_TASKS = 50;
    private static final int NUMBER_OF_ITERATIONS = 20;
    private final Path tempDir = Fixtures.tempDirectory();
    private final SubscriberFactory subscriberFactory = new SubscriberFactory();
    private PageCache pageCache;
    private PublisherFactory factory;
    private ExecutorService executorService;

    @Before
    public void before() throws Exception
    {
        pageCache = PageCache.create(tempDir, 4096);
        factory = new PublisherFactory(pageCache);
        executorService = newFixedThreadPool(2);
    }

    @Test
    public void shouldDispatchMessages() throws Exception
    {
        final TestTopicMessageCounter testTopicMessageCount = new TestTopicMessageCounter();
        final Subscriber testTopicSubscriber =
                subscriberFactory.getSubscriber(TestTopic.class, testTopicMessageCount);
        final OtherTopicMessageCounter otherTopicMessageCount = new OtherTopicMessageCounter();
        final Subscriber otherTopicSubscriber = subscriberFactory.getSubscriber(OtherTopic.class, otherTopicMessageCount);

        final TestTopic proxy = factory.getPublisherProxy(TestTopic.class);
        final OtherTopic paramTester = factory.getPublisherProxy(OtherTopic.class);
        final int expectedMessagesPerTopic = NUMBER_OF_TASKS * NUMBER_OF_ITERATIONS;
        final CountDownLatch taskLatch = new CountDownLatch(NUMBER_OF_TASKS * 2);

        for (int i = 0; i < NUMBER_OF_TASKS; i++)
        {
            executorService.submit(() -> {
                try
                {
                    for (int j = 0; j < NUMBER_OF_ITERATIONS; j++)
                    {
                        if ((System.nanoTime() & 1L) == 0)
                        {
                            proxy.say("hola", 7);
                        }
                        else
                        {
                            proxy.say("bonjour", 11);
                        }
                    }
                }
                catch (RuntimeException e)
                {
                    e.printStackTrace();
                }
                taskLatch.countDown();
            });
            executorService.submit(() -> {
                try
                {
                    for (int j = 0; j < NUMBER_OF_ITERATIONS; j++)
                    {
                        if ((System.nanoTime() & 1L) == 0)
                        {
                            paramTester.testParams(false, (byte) -5, (short) -7, -11,
                                    Float.MAX_VALUE, Long.MIN_VALUE, Double.POSITIVE_INFINITY, "first", "second");
                        }
                        else
                        {
                            paramTester.testParams(true, (byte) 5, (short) 7, 11,
                                    13.7f, 17L, 19.37d, "first", "second");
                        }
                    }
                }
                catch (RuntimeException e)
                {
                    e.printStackTrace();
                }
                taskLatch.countDown();
            });
        }

        assertTrue(taskLatch.await(20, TimeUnit.SECONDS));
        executorService.shutdownNow();
        assertTrue(executorService.awaitTermination(20, TimeUnit.SECONDS));

        final Int2ObjectHashMap<Subscriber> subscriberMap =
                new Int2ObjectHashMap<>();

        subscriberMap.put(testTopicSubscriber.getTopicId(), testTopicSubscriber);
        subscriberMap.put(otherTopicSubscriber.getTopicId(), otherTopicSubscriber);

        final TopicDispatcherRecordHandler topicDispatcher =
                new TopicDispatcherRecordHandler(subscriberMap);

        new StreamingReader(pageCache, topicDispatcher, false, true).process();


        assertThat(testTopicMessageCount.getMessageCount(), is(expectedMessagesPerTopic));
        assertThat(otherTopicMessageCount.getMessageCount(), is(expectedMessagesPerTopic));
    }

    @After
    public void after() throws Exception
    {
        executorService.shutdownNow();
    }
}