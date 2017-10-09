package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.buffer.Fixtures;
import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.factory.Service;
import com.aitusoftware.transport.factory.ServiceFactory;
import com.aitusoftware.transport.factory.SubscriberDefinition;
import com.aitusoftware.transport.messaging.TopicDispatcherRecordHandler;
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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;


@Ignore
public final class ServiceIntegrationTest
{
    private Service service;
    private MarketData marketDataPublisher;
    private ExecutorService executor;
    private CountDownLatch latch;

    @Before
    public void setUp() throws Exception
    {
        final Path path = Fixtures.tempDirectory();

        final ServiceFactory serviceFactory = new ServiceFactory(path);
        final TraderBot traderBot = new TraderBot(serviceFactory.createPublisher(OrderNotifications.class));
        serviceFactory.registerSubscriber(new SubscriberDefinition<>(MarketData.class, traderBot));
        serviceFactory.registerSubscriber(new SubscriberDefinition<>(MarketNews.class, traderBot));
        serviceFactory.registerSubscriber(new SubscriberDefinition<>(TradeNotifications.class, traderBot));
        this.service = serviceFactory.create();

        marketDataPublisher = new PublisherFactory(serviceFactory.getSubscriberPageCache()).getPublisherProxy(MarketData.class);
        final PageCache subscriberPageCache = serviceFactory.getPublisherPageCache();
        this.latch = new CountDownLatch(1);

        final Subscriber<OrderNotifications> subscriber = new SubscriberFactory().getSubscriber(OrderNotifications.class, new OrderNotifications()
        {
            @Override
            public void limitOrder(final CharSequence symbol, final CharSequence orderId, final boolean isBid, final long quantity, final double price, final int ecnId)
            {
                latch.countDown();
            }

            @Override
            public void marketOrder(final CharSequence symbol, final CharSequence orderId, final boolean isBid, final long quantity, final int ecnId)
            {
                latch.countDown();
            }

            @Override
            public void cancelOrder(final CharSequence orderId, final int ecnId)
            {
                latch.countDown();
            }
        });

        final Int2ObjectHashMap<Subscriber> subscriberMap = new Int2ObjectHashMap<>();
        subscriberMap.put(subscriber.getTopicId(), subscriber);
        final StreamingReader streamingReader = new StreamingReader(subscriberPageCache, new TopicDispatcherRecordHandler(subscriberMap), true, true);
        executor = Executors.newSingleThreadExecutor();
        executor.execute(streamingReader::process);
    }

    @Test
    public void shouldHandleMessages() throws Exception
    {
        for (int i = 0; i < 20; i++)
        {
            marketDataPublisher.onAsk("USD/EUR", i, 17 * i, 37);
        }
        this.service.start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @After
    public void tearDown() throws Exception
    {
        executor.shutdownNow();
        assertTrue(service.stop(5, TimeUnit.SECONDS));
    }
}
