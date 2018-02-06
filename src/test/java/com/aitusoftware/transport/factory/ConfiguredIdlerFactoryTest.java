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
package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.integration.MarketNews;
import com.aitusoftware.transport.integration.OrderNotifications;
import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.WritableByteChannel;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ConfiguredIdlerFactoryTest
{
    private static final String PREFIX = "transport.publisher.idler.";
    private final Properties properties = new Properties();
    private final Idler fallback = Idlers.staticPause(1, TimeUnit.MILLISECONDS);
    private final ConfiguredIdlerFactory factory =
            new ConfiguredIdlerFactory(PREFIX, properties::getProperty, fallback);

    @Before
    public void setUp()
    {
        properties.setProperty(PREFIX + MarketNews.class.getName(), "ADAPTIVE,5,MILLISECONDS");
    }

    @Test
    public void shouldCreateFromProperty()
    {
        final Idler idler = factory.apply(MarketNews.class);
        assertNotNull(idler);
        assertThat(idler, is(not(sameInstance(fallback))));
    }

    @Test
    public void shouldUseFallbackIfPropertyIsNotDefined()
    {
        final Idler idler = factory.apply(OrderNotifications.class);
        assertNotNull(idler);
        assertThat(idler, is(sameInstance(fallback)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldBlowUpIfArgumentDoesNotDefineTopic()
    {
        factory.apply(WritableByteChannel.class);
    }
}