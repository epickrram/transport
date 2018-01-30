package com.aitusoftware.transport.threads;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IdlersTest
{
    @Test
    public void shouldCreateFromString()
    {
        assertThat(Idlers.forString("BUSY_SPIN").getClass(), is(equalTo(BusySpinIdler.class)));
        assertThat(Idlers.forString("YIELDING").getClass(), is(equalTo(YieldingIdler.class)));
        assertThat(Idlers.forString("STATIC,1,MILLISECONDS").getClass(), is(equalTo(StaticPausingIdler.class)));
        assertThat(Idlers.forString("ADAPTIVE,1,MILLISECONDS").getClass(), is(equalTo(AdaptiveIdler.class)));
    }
}