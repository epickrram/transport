package com.aitusoftware.transport.buffer;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SlabTest {
    private final Slab slab = new Slab(ByteBuffer.allocateDirect(64));

    @Test
    public void shouldCasAtOffset() throws Exception {
        assertThat(slab.getLongVolatile(8), is(0L));
        assertThat(slab.compareAndSetLong(8, 0L, 17L), is(true));
        assertThat(slab.getLongVolatile(8), is(17L));
    }

    @Test
    public void shouldGetAndAddAtOffset() throws Exception {
        assertThat(slab.getLongVolatile(8), is(0L));
        assertThat(slab.getAndAddLong(8, 17L), is(0L));
        assertThat(slab.getAndAddLong(8, 11L), is(17L));
        assertThat(slab.getLongVolatile(8), is(28L));
    }
}