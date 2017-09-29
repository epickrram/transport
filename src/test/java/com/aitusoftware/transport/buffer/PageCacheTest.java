package com.aitusoftware.transport.buffer;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PageCacheTest
{
    private static final int PAGE_SIZE = 4096;
    private static final int MESSAGE_SIZE = PAGE_SIZE / 23;
    private static final int PADDED_MESSAGE_SIZE = Offsets.getAlignedPosition(MESSAGE_SIZE);
    private static final int MESSAGES_PER_PAGE = PAGE_SIZE / PADDED_MESSAGE_SIZE;
    private static final int WASTED_PAGE_SPACE = PAGE_SIZE - (MESSAGES_PER_PAGE * PADDED_MESSAGE_SIZE);
    private static final int MESSAGE_COUNT = 128;
    private static final int PAGE_COUNT = 128 / 23;

    private final PageCache pageCache = PageCache.create(Fixtures.tempDirectory(), PAGE_SIZE);
    private final ByteBuffer message = ByteBuffer.allocate(MESSAGE_SIZE);

    @Test
    public void shouldAppendDataOverSeveralPages() throws Exception
    {
        Fixtures.writeMessages(message, pageCache, MESSAGE_COUNT);

        assertThat(pageCache.estimateTotalLength(), is((long) MESSAGE_COUNT * PADDED_MESSAGE_SIZE + (PAGE_COUNT + 1) * WASTED_PAGE_SPACE));
    }

}