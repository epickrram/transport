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

public class PropertiesBackedIdlerFactoryTest
{
    private final Properties properties = new Properties();
    private final Idler fallback = Idlers.staticPause(1, TimeUnit.MILLISECONDS);
    private final PropertiesBackedIdlerFactory factory =
            new PropertiesBackedIdlerFactory(properties, fallback);

    @Before
    public void setUp()
    {
        properties.setProperty(MarketNews.class.getName(), "ADAPTIVE,5,MILLISECONDS");
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