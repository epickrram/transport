package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.integration.MarketNews;
import com.aitusoftware.transport.integration.OrderNotifications;
import com.aitusoftware.transport.integration.TradeNotifications;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class ConfiguredAffinityWrapperTest
{
    private static final String PREFIX = "transport.publisher.idler.";
    private final Properties properties = new Properties();
    private final ConfiguredAffinityWrapper factory =
            new ConfiguredAffinityWrapper(PREFIX, properties::getProperty);
    private boolean failed;
    private Runnable delegate;

    @Before
    public void setUp()
    {
        properties.setProperty(PREFIX + MarketNews.class.getName(), "-20");
        properties.setProperty(PREFIX + OrderNotifications.class.getName(), "FOO");
    }

    @Test
    public void shouldNotWrapWhenAffinityIsNegative()
    {
        delegate = this::dummy;
        final Runnable actual = factory.wrap(delegate, MarketNews.class, this::onException);

        assertThat(actual, is(sameInstance(delegate)));
        assertThat(failed, is(true));
    }

    @Test
    public void shouldNotWrapWhenAffinityIsNotNumber()
    {
        delegate = this::dummy;
        final Runnable actual = factory.wrap(delegate, OrderNotifications.class, this::onException);

        assertThat(actual, is(sameInstance(delegate)));
        assertThat(failed, is(true));
    }

    @Test
    public void shouldNotWrapWhenAffinityIsNull()
    {
        delegate = this::dummy;
        final Runnable actual = factory.wrap(delegate, TradeNotifications.class, this::onException);

        assertThat(actual, is(sameInstance(delegate)));
        assertThat(failed, is(true));
    }

    private void dummy()
    {
        // this method does nothing
    }

    private void onException(@SuppressWarnings("unused") final Throwable t)
    {
        failed = true;
    }
}