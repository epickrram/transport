package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.buffer.Fixtures;
import com.aitusoftware.transport.factory.Service;
import com.aitusoftware.transport.factory.ServiceFactory;
import com.aitusoftware.transport.factory.SubscriberDefinition;
import com.aitusoftware.transport.net.AddressSpace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class OneToManyServiceIntegrationTest
{
    private static final int RECEIVING_SERVICE_COUNT = 10;
    private final OrderGateway[] receivers =  new OrderGateway[RECEIVING_SERVICE_COUNT];
    private final CountingTradeNotifications tradeNotifications = new CountingTradeNotifications();
    private OrderNotifications publisher;

    @Before
    public void setUp() throws Exception
    {
        final Path publishingServicePath = Fixtures.tempDirectory();

        final MultiSocketAddressSpace testAddressSpace =
                new MultiSocketAddressSpace(RECEIVING_SERVICE_COUNT);

        for (int i = 0; i < RECEIVING_SERVICE_COUNT; i++)
        {
            final Path orderGatewayPath = Fixtures.tempDirectory();
            final ServiceFactory gatewayServiceFactory = new ServiceFactory(orderGatewayPath,
                    new FixedServerSocketFactory(testAddressSpace.forIndex(i)), testAddressSpace);
            receivers[i] = new OrderGateway(tradeNotifications);
            gatewayServiceFactory.registerSubscriber(
                    new SubscriberDefinition<>(OrderNotifications.class, receivers[i], null));
            gatewayServiceFactory.create().start();
        }

        final ServiceFactory publishingServiceFactory = new ServiceFactory(publishingServicePath,
                new FixedServerSocketFactory(ServerSocketChannel.open()), testAddressSpace);
        publisher = publishingServiceFactory.createPublisher(OrderNotifications.class);
        final Service publisherService = publishingServiceFactory.create();
        publisherService.start();
    }

    @Ignore
    @Test
    public void shouldAcceptMultipleInboundConnections() throws Exception
    {
        for (int i = 0; i < 40; i++)
        {
            publisher.limitOrder("test-" + i, "order-" + i, true, 17L, 3.14D, 37);
        }

        if (!tradeNotifications.latch.await(5, TimeUnit.SECONDS))
        {
            Assert.fail(String.format("Did not receive expected number of messages. Number remaining: %d%n",
                    tradeNotifications.latch.getCount()));
        }
    }

    private static final class CountingTradeNotifications implements TradeNotifications
    {
        private final CountDownLatch latch = new CountDownLatch(30);

        @Override
        public void onOrderAccepted(final CharSequence symbol, final CharSequence orderId, final boolean isBid, final long matchedQuantity,
                                    final long remainingQuantity, final double price, final int ecnId)
        {
            latch.countDown();
        }

        @Override
        public void onOrderRejected(final CharSequence symbol, final CharSequence orderId, final int ecnId, final int rejectionReason)
        {
            latch.countDown();
        }
    }

    private static final class MultiSocketAddressSpace implements AddressSpace
    {
        private final ServerSocketChannel[] channels;

        MultiSocketAddressSpace(final int numberOfReceivers) throws IOException
        {
            this.channels = new ServerSocketChannel[numberOfReceivers];
            for (int i = 0; i < channels.length; i++)
            {
                channels[i] = ServerSocketChannel.open().bind(null);
            }
        }

        @Override
        public List<SocketAddress> addressesOf(final Class<?> topicClass)
        {
            final List<SocketAddress> addresses = new ArrayList<>(channels.length);
            for (final ServerSocketChannel channel : channels)
            {
                try
                {
                    addresses.add(channel.getLocalAddress());
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
            }
            return addresses;
        }

        @Override
        public int portOf(final Class<?> topicClass)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String hostOf(final Class<?> topicClass)
        {
            throw new UnsupportedOperationException();
        }

        ServerSocketChannel forIndex(final int i)
        {
            return channels[i];
        }
    }
}
