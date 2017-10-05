package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.buffer.Fixtures;
import com.aitusoftware.transport.factory.Service;
import com.aitusoftware.transport.factory.ServiceFactory;
import com.aitusoftware.transport.factory.SubscriberDefinition;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

@Ignore
public final class ServiceIntegrationTest
{
    private Service service;

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
        this.service.start();
    }

    @After
    public void tearDown() throws Exception
    {
        assertTrue(service.stop(5, TimeUnit.SECONDS));
    }
}
