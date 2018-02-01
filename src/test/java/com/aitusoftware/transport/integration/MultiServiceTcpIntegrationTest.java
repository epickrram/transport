package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.Fixtures;
import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.factory.Media;
import com.aitusoftware.transport.factory.Service;
import com.aitusoftware.transport.factory.ServiceFactory;
import com.aitusoftware.transport.factory.SubscriberDefinition;
import com.aitusoftware.transport.factory.SubscriberThreading;
import com.aitusoftware.transport.messaging.StaticAddressSpace;
import com.aitusoftware.transport.messaging.proxy.PublisherFactory;
import com.aitusoftware.transport.net.AddressSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static com.aitusoftware.transport.Fixtures.testIdler;
import static org.junit.Assert.assertTrue;

public final class MultiServiceTcpIntegrationTest
{
    private final Media media = Media.TCP;
    private Service traderBotService;
    private MarketData marketDataPublisher;
    private Service orderGatewayService;
    private TraderBot traderBot;

    @Before
    public void setUp() throws Exception
    {
        final Path traderBotPath = Fixtures.tempDirectory();
        final Path orderGatewayPath = Fixtures.tempDirectory();

        final ServerSocketChannel traderBotListenAddr = ServerSocketChannel.open();
        traderBotListenAddr.configureBlocking(false);
        traderBotListenAddr.bind(null);

        final ServerSocketChannel orderGatewayListenAddr = ServerSocketChannel.open();
        orderGatewayListenAddr.configureBlocking(false);
        orderGatewayListenAddr.bind(null);
        final AddressSpace testAddressSpace = new DelegatingAddressSpace(new StaticAddressSpace(),
                traderBotListenAddr.socket().getLocalPort(),
                orderGatewayListenAddr.socket().getLocalPort());

        final ServiceFactory traderBotServiceFactory = new ServiceFactory(traderBotPath,
                new FixedServerSocketFactory(traderBotListenAddr), testAddressSpace, testIdler(), SubscriberThreading.SINGLE_THREADED);
        traderBot = new TraderBot(traderBotServiceFactory.createPublisher(OrderNotifications.class, media));
        traderBotServiceFactory.registerRemoteSubscriber(
                new SubscriberDefinition<>(MarketData.class, traderBot, media));
        traderBotServiceFactory.registerRemoteSubscriber(
                new SubscriberDefinition<>(MarketNews.class, traderBot, media));
        traderBotServiceFactory.registerRemoteSubscriber(
                new SubscriberDefinition<>(TradeNotifications.class, traderBot, media));
        this.traderBotService = traderBotServiceFactory.create();

        final ServiceFactory orderGatewayServiceFactory = new ServiceFactory(orderGatewayPath,
                new FixedServerSocketFactory(orderGatewayListenAddr), testAddressSpace, testIdler(), SubscriberThreading.SINGLE_THREADED);
        final OrderGateway orderGateway = new OrderGateway(orderGatewayServiceFactory.createPublisher(TradeNotifications.class, media));
        orderGatewayServiceFactory.registerRemoteSubscriber(
                new SubscriberDefinition<>(OrderNotifications.class, orderGateway, media));
        this.orderGatewayService = orderGatewayServiceFactory.create();

        final PageCache inputPageCache = PageCache.create(traderBotPath.resolve(ServiceFactory.SUBSCRIBER_PAGE_CACHE_PATH), ServiceFactory.PAGE_SIZE);
        marketDataPublisher = new PublisherFactory(inputPageCache).getPublisherProxy(MarketData.class);

        this.traderBotService.start();
        this.orderGatewayService.start();
    }

    @Test
    public void shouldHandleMessages() throws Exception
    {
        for (int i = 0; i < 20; i++)
        {
            marketDataPublisher.onAsk("USD/EUR", i, 17 * i, 37);
        }

        // asserts that a message is round-tripped through multiple services
        assertTrue(traderBot.getOrderAcceptedLatch().await(5, TimeUnit.SECONDS));
    }

    @After
    public void tearDown() throws Exception
    {
        assertTrue(traderBotService.stop(5, TimeUnit.SECONDS));
        assertTrue(orderGatewayService.stop(5, TimeUnit.SECONDS));
    }

}