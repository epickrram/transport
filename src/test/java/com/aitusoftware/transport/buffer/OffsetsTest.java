package com.aitusoftware.transport.buffer;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OffsetsTest {
    private final Offsets offsets = new Offsets(64);

    @Test
    public void shouldCalculateOffset() throws Exception {
        assertThat(offsets.pageOffset(0L), is(0));
        assertThat(offsets.pageOffset(1L), is(1));
        assertThat(offsets.pageOffset(63L), is(63));
        assertThat(offsets.pageOffset(64L), is(0));
        assertThat(offsets.pageOffset(65L), is(1));
        assertThat(offsets.pageOffset(127L), is(63));
        assertThat(offsets.pageOffset(128L), is(0));
        assertThat(offsets.pageOffset(129L), is(1));
    }

    @Test
    public void shouldCalculatePageNumber() throws Exception {
        assertThat(offsets.pageNumber(0L), is(0));
        assertThat(offsets.pageNumber(63L), is(0));
        assertThat(offsets.pageNumber(64L), is(1));
        assertThat(offsets.pageNumber(65L), is(1));
        assertThat(offsets.pageNumber(127L), is(1));
        assertThat(offsets.pageNumber(128L), is(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequirePowerOfTwo() throws Exception {
        new Offsets(65);
    }
}