package com.aitusoftware.transport.buffer;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class PageTest
{
    private final Page page = new Page(SlabFactory.createSlab(8192), 0, null);

    @Test
    public void shouldPutAndRetrieveRecord() throws Exception
    {
        final ByteBuffer buffer = ByteBuffer.allocate(8);
        for (long i = 0; i < 100; i++)
        {
            encode(i, buffer);

            page.write(buffer);
        }

        int position = 0;
        for (long i = 0; i < 100; i++)
        {
            buffer.clear();

            final int header = page.header(position);
            assertThat(Page.isReady(header), is(true));
            final int recordLength = Page.recordLength(header);
            buffer.limit(recordLength);
            page.read(position, buffer);

            assertThat(decode(buffer), is(i));

            position += recordLength + Record.HEADER_LENGTH;
            position = Offsets.getAlignedPosition(position);
        }
    }

    @Test
    public void shouldIndicateThatPageFreeSpaceIsExhausted() throws Exception
    {
        final ByteBuffer data = ByteBuffer.allocate(5000);
        assertThat(page.write(data), is(WriteResult.SUCCESS));
        data.clear();
        assertThat(page.write(data), is(WriteResult.NOT_ENOUGH_SPACE));
    }

    @Test
    public void shouldNotBeReady() throws Exception
    {
        assertThat(Page.isReady(Page.CLAIMED_MARKER), is(false));
    }

    @Test
    public void shouldBeReady() throws Exception
    {
        assertThat(Page.isReady(Page.CLAIMED_MARKER | Page.READY_MARKER), is(true));
    }

    private static long decode(final ByteBuffer source)
    {
        return source.getLong();
    }

    private static void encode(final long payload, final ByteBuffer target)
    {
        target.clear();
        target.putLong(payload);
        target.flip();
    }
}