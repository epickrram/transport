package com.aitusoftware.transport.memory;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ReferenceCounterTest
{
    private final ReferenceCounter counter = new ReferenceCounter();

    @Test
    public void shouldMaintainReferenceCount() throws Exception
    {
        assertTrue(counter.claim());
        assertTrue(counter.claim());
        assertTrue(counter.claim());

        assertThat(counter.getReferenceCount(), is(3));

        counter.release();
        assertThat(counter.getReferenceCount(), is(2));

        counter.release();
        assertThat(counter.getReferenceCount(), is(1));

        counter.release();
        assertThat(counter.getReferenceCount(), is(0));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBlowUpOnDoubleFree() throws Exception
    {
        counter.release();
    }

    @Test
    public void shouldNotBeAbleToMakeUnreachableWhenReferencesExist() throws Exception
    {
        counter.claim();

        assertFalse(counter.makeUnreachable());
    }

    @Test
    public void shouldBeAbleToMakeUnreachable() throws Exception
    {
        assertTrue(counter.makeUnreachable());

        assertFalse(counter.claim());
        assertFalse(counter.claim());
        assertFalse(counter.claim());
    }

    @Test(expected = IllegalStateException.class)
    public void releaseWhenUnreachable() throws Exception
    {
        counter.makeUnreachable();
        counter.release();
    }
}