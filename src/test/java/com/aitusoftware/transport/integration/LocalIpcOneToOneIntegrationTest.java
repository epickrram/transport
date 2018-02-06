/*
 * Copyright 2017 - 2018 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.Fixtures;
import com.aitusoftware.transport.StaticAddressSpace;
import com.aitusoftware.transport.factory.Media;
import com.aitusoftware.transport.factory.Service;
import com.aitusoftware.transport.factory.ServiceFactory;
import com.aitusoftware.transport.factory.SubscriberDefinition;
import com.aitusoftware.transport.factory.SubscriberThreading;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.aitusoftware.transport.Fixtures.testIdlerFactory;
import static org.junit.Assert.assertTrue;

public final class LocalIpcOneToOneIntegrationTest
{
    private static final int MESSAGE_COUNT = 20;
    private final Media media = Media.TCP;
    private Service receiverService;
    private MarketData marketDataPublisher;
    private CountDownLatch latch;

    @Before
    public void setUp() throws Exception
    {
        final Path receiverPath = Fixtures.tempDirectory();
        final Path senderPath = Fixtures.tempDirectory();

        final ServiceFactory receiverServiceFactory =
                new ServiceFactory(receiverPath, new FixedServerSocketFactory(ServerSocketChannel.open()),
                        new StaticAddressSpace(), testIdlerFactory(), SubscriberThreading.SINGLE_THREADED);

        final ServiceFactory senderServiceFactory =
                new ServiceFactory(senderPath, new FixedServerSocketFactory(ServerSocketChannel.open()),
                        new StaticAddressSpace(), testIdlerFactory(), SubscriberThreading.SINGLE_THREADED);

        this.latch = new CountDownLatch(MESSAGE_COUNT);
        final MarketDataReceiver marketDataReceiver = new MarketDataReceiver(latch);
        receiverServiceFactory.registerLocalSubscriber(new SubscriberDefinition<>(MarketData.class,
                        marketDataReceiver, media),
                senderPath.resolve(ServiceFactory.PUBLISHER_PAGE_CACHE_PATH));
        this.receiverService = receiverServiceFactory.create();


        marketDataPublisher = senderServiceFactory.createPublisher(MarketData.class);
        this.receiverService.start();
    }

    @Test
    public void shouldHandleMessages() throws Exception
    {
        for (int i = 0; i < MESSAGE_COUNT; i++)
        {
            marketDataPublisher.onAsk("USD/EUR", i, 17 * i, 37);
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @After
    public void tearDown() throws Exception
    {
        assertTrue(receiverService.stop(5, TimeUnit.SECONDS));
    }

    private static final class MarketDataReceiver implements MarketData
    {
        private final CountDownLatch latch;

        private MarketDataReceiver(final CountDownLatch latch)
        {
            this.latch = latch;
        }

        @Override
        public void onAsk(final CharSequence symbol, final long quantity, final double price, final int sourceId)
        {
            latch.countDown();
        }

        @Override
        public void onBid(final CharSequence symbol, final long quantity, final double price, final int sourceId)
        {

        }

        @Override
        public void onTrade(final CharSequence symbol, final boolean isBuy, final long quantity, final double price, final int sourceId)
        {

        }
    }
}
