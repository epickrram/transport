package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.Fixtures;
import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.factory.Media;
import com.aitusoftware.transport.factory.Service;
import com.aitusoftware.transport.factory.ServiceFactory;
import com.aitusoftware.transport.factory.SubscriberDefinition;
import com.aitusoftware.transport.factory.SubscriberThreading;
import com.aitusoftware.transport.messaging.StaticAddressSpace;
import com.aitusoftware.transport.messaging.TopicDispatcherRecordHandler;
import com.aitusoftware.transport.messaging.proxy.PublisherFactory;
import com.aitusoftware.transport.messaging.proxy.Subscriber;
import com.aitusoftware.transport.messaging.proxy.SubscriberFactory;
import com.aitusoftware.transport.reader.StreamingReader;
import com.aitusoftware.transport.threads.Idlers;
import org.agrona.collections.Int2ObjectHashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.aitusoftware.transport.Fixtures.testIdlerFactory;
import static org.junit.Assert.assertTrue;

public final class LocalIpcOneToOneIntegrationTest
{
    private final Media media = Media.TCP;
    private Service service;
    private MarketData marketDataPublisher;
    private ExecutorService executor;
    private CountDownLatch latch;

    @Before
    public void setUp() throws Exception
    {
        final Path path = Fixtures.tempDirectory();

        final ServiceFactory serviceFactory =
                new ServiceFactory(path, new FixedServerSocketFactory(ServerSocketChannel.open()),
                        new StaticAddressSpace(), testIdlerFactory(), SubscriberThreading.SINGLE_THREADED);
        final TraderBot traderBot = new TraderBot(serviceFactory.createPublisher(OrderNotifications.class, media));
        serviceFactory.registerRemoteSubscriber(new SubscriberDefinition<>(MarketData.class, traderBot, media));
        serviceFactory.registerRemoteSubscriber(new SubscriberDefinition<>(MarketNews.class, traderBot, media));
        serviceFactory.registerRemoteSubscriber(new SubscriberDefinition<>(TradeNotifications.class, traderBot, media));
        this.service = serviceFactory.create();
        final PageCache inputPageCache = PageCache.create(path.resolve(ServiceFactory.SUBSCRIBER_PAGE_CACHE_PATH), ServiceFactory.PAGE_SIZE);
        marketDataPublisher = new PublisherFactory(inputPageCache).getPublisherProxy(MarketData.class);
        final PageCache outputPageCache = PageCache.create(path.resolve(ServiceFactory.PUBLISHER_PAGE_CACHE_PATH), ServiceFactory.PAGE_SIZE);
        this.latch = new CountDownLatch(1);

        final Subscriber<OrderNotifications> subscriber = new SubscriberFactory().getSubscriber(OrderNotifications.class, new EventReceiver());

        final Int2ObjectHashMap<Subscriber> subscriberMap = new Int2ObjectHashMap<>();
        subscriberMap.put(subscriber.getTopicId(), subscriber);
        final StreamingReader streamingReader = new StreamingReader(outputPageCache, new TopicDispatcherRecordHandler(subscriberMap), true, Idlers.staticPause(1, TimeUnit.MILLISECONDS));
        executor = Executors.newSingleThreadExecutor();
        executor.execute(streamingReader::process);
        this.service.start();
    }

    @Test
    public void shouldHandleMessages() throws Exception
    {
        for (int i = 0; i < 20; i++)
        {
            marketDataPublisher.onAsk("USD/EUR", i, 17 * i, 37);
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @After
    public void tearDown() throws Exception
    {
        assertTrue(service.stop(5, TimeUnit.SECONDS));
        executor.shutdownNow();
    }

    private class EventReceiver implements OrderNotifications
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
    }

}
