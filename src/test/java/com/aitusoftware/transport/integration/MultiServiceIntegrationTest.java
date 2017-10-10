package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.buffer.Fixtures;
import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.factory.Service;
import com.aitusoftware.transport.factory.ServiceFactory;
import com.aitusoftware.transport.factory.SubscriberDefinition;
import com.aitusoftware.transport.messaging.proxy.PublisherFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

@Ignore
public final class MultiServiceIntegrationTest
{
    private static final InetSocketAddress ORDER_GATEWAY_CONNECT_ADDR =
            new InetSocketAddress("127.0.0.1", 15001);
    private static final InetSocketAddress TRADER_BOT_CONNECT_ADDR =
            new InetSocketAddress("127.0.0.1", 16001);
    private static final InetSocketAddress ORDER_GATEWAY_LISTEN_ADDR =
            new InetSocketAddress("0.0.0.0", ORDER_GATEWAY_CONNECT_ADDR.getPort());
    private static final InetSocketAddress TRADER_BOT_LISTEN_ADDR =
            new InetSocketAddress("0.0.0.0", TRADER_BOT_CONNECT_ADDR.getPort());
    private Service traderBotService;
    private MarketData marketDataPublisher;
    private Service orderGatewayService;
    private TraderBot traderBot;
    private ServiceFactory orderGatewayServiceFactory;
    private ServiceFactory traderBotServiceFactory;

    @Before
    public void setUp() throws Exception
    {
        final Path traderBotPath = Fixtures.tempDirectory();
        final Path orderGatewayPath = Fixtures.tempDirectory();

        traderBotServiceFactory = new ServiceFactory(traderBotPath);
        traderBot = new TraderBot(traderBotServiceFactory.createPublisher(OrderNotifications.class));
        traderBotServiceFactory.registerSubscriber(
                new SubscriberDefinition<>(MarketData.class, traderBot, TRADER_BOT_LISTEN_ADDR));
        traderBotServiceFactory.registerSubscriber(
                new SubscriberDefinition<>(MarketNews.class, traderBot, TRADER_BOT_LISTEN_ADDR));
        traderBotServiceFactory.registerSubscriber(
                new SubscriberDefinition<>(TradeNotifications.class, traderBot, TRADER_BOT_LISTEN_ADDR));
        traderBotServiceFactory.registerRemoteListenerTo(OrderNotifications.class, ORDER_GATEWAY_CONNECT_ADDR);
        this.traderBotService = traderBotServiceFactory.create();

        orderGatewayServiceFactory = new ServiceFactory(orderGatewayPath);
        final OrderGateway orderGateway = new OrderGateway(orderGatewayServiceFactory.createPublisher(TradeNotifications.class));
        orderGatewayServiceFactory.registerSubscriber(
                new SubscriberDefinition<>(OrderNotifications.class, orderGateway, ORDER_GATEWAY_LISTEN_ADDR));
        orderGatewayServiceFactory.registerRemoteListenerTo(TradeNotifications.class, TRADER_BOT_CONNECT_ADDR);
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

        assertTrue(traderBot.getOrderAcceptedLatch().await(5, TimeUnit.SECONDS));
    }

    @After
    public void tearDown() throws Exception
    {
        assertTrue(traderBotService.stop(5, TimeUnit.SECONDS));
        assertTrue(orderGatewayService.stop(5, TimeUnit.SECONDS));
    }
}