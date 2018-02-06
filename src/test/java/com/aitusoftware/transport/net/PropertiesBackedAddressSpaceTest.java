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
package com.aitusoftware.transport.net;

import com.aitusoftware.transport.integration.MarketNews;
import com.aitusoftware.transport.integration.OrderNotifications;
import com.aitusoftware.transport.integration.TradeNotifications;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.WritableByteChannel;
import java.util.Properties;

import static java.util.List.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PropertiesBackedAddressSpaceTest
{
    private static final String NEWS_FIRST_ADDRESS = "127.0.0.1:14768";
    private static final String NEWS_SECOND_ADDRESS = "127.0.0.1:12345";
    private static final String MARKET_NEWS_ADDRESS_SPEC = String.format("%s,%s",
            NEWS_FIRST_ADDRESS, NEWS_SECOND_ADDRESS);
    private static final String ORDER_EVENTS_ADDRESS = "0.0.0.0:56748";
    private final Properties properties = new Properties();
    private final PropertiesBackedAddressSpace addressSpace =
            new PropertiesBackedAddressSpace(properties);

    @Before
    public void setUp()
    {
        properties.put(MarketNews.class.getName(), MARKET_NEWS_ADDRESS_SPEC);
        properties.put(OrderNotifications.class.getName(), ORDER_EVENTS_ADDRESS);
        properties.put(WritableByteChannel.class.getName(), ORDER_EVENTS_ADDRESS);
    }

    @Test
    public void shouldResolveMultipleAddressesFromSpecifiedProperties()
    {
        assertThat(addressSpace.addressesOf(MarketNews.class),
                is(of(socketAddress(NEWS_FIRST_ADDRESS),
                        socketAddress(NEWS_SECOND_ADDRESS))));
    }

    @Test
    public void shouldResolveSingleAddressFromSpecifiedProperties()
    {
        assertThat(addressSpace.addressesOf(OrderNotifications.class),
                is(of(socketAddress(ORDER_EVENTS_ADDRESS))));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldBlowUpWhenAccessingSingleAddressIfMultipleAddressesAreSpecified()
    {
        addressSpace.addressOf(MarketNews.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldBlowUpIfTopicClassIsNotSpecified()
    {
        addressSpace.addressesOf(TradeNotifications.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldBlowUpIfClassDoesNotDefineTopic()
    {
        addressSpace.addressesOf(WritableByteChannel.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotSupportHostOf()
    {
        addressSpace.hostOf(MarketNews.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotSupportPortOf()
    {
        addressSpace.portOf(MarketNews.class);
    }

    private SocketAddress socketAddress(final String spec)
    {
        final String[] tokens = spec.split(":");
        return new InetSocketAddress(tokens[0], Integer.parseInt(tokens[1]));
    }
}