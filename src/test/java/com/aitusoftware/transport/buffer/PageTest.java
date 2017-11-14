package com.aitusoftware.transport.buffer;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class PageTest
{
    private static final int PAGE_SIZE = 8192;
    private final Page page = new Page(SlabFactory.createSlab(PAGE_SIZE), 0, null);

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

    @Test
    public void shouldFillPageEntirelyWithSingleMessage() throws Exception
    {
        final int maximumPayloadSize = PAGE_SIZE - Record.HEADER_LENGTH - PageHeader.HEADER_SIZE;

        assertThat(page.acquireSpaceInBuffer(maximumPayloadSize), is(0));
    }

    @Test
    public void shouldFillPageEntirelyWithMultipleMessages() throws Exception
    {
        final int maximumPayloadSize = PAGE_SIZE - Record.HEADER_LENGTH - PageHeader.HEADER_SIZE;
        final int firstMessageLength = maximumPayloadSize - 500;
        // padding to cache alignment
        final int padding = 64 - (firstMessageLength % 64);
        final int secondMessageLength = 500 - Record.HEADER_LENGTH - padding;

        assertThat(page.acquireSpaceInBuffer(firstMessageLength), is(0));
        assertThat(page.acquireSpaceInBuffer(secondMessageLength), is(firstMessageLength + padding));
    }

    @Test
    public void shouldIndicateMessageTooLarge() throws Exception
    {
        final int maximumPayloadSize = PAGE_SIZE - Record.HEADER_LENGTH - PageHeader.HEADER_SIZE;

        assertThat(page.acquireSpaceInBuffer(maximumPayloadSize + 1), is(Page.ERR_NOT_ENOUGH_SPACE));
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