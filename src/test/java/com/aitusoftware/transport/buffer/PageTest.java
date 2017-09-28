package com.aitusoftware.transport.buffer;

import org.junit.Test;

import java.nio.ByteBuffer;

import static com.aitusoftware.transport.buffer.SlabFactory.SLAB_FACTORY;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class PageTest {
    private final Page page = new Page(SLAB_FACTORY.createSlab(8192));

    @Test
    public void shouldPutAndRetrieveRecord() throws Exception {
        final ByteBuffer buffer = ByteBuffer.allocate(8);
        for (long i = 0; i < 100; i++) {
            encode(i, buffer);

            page.write(buffer);
        }

        long position = 0;
        for (long i = 0; i < 100; i++) {
            buffer.clear();

            final int header = page.header(position);
            assertThat(Page.isReady(header), is(true));
            final int recordLength = Page.recordLength(header);
            buffer.limit(recordLength);
            page.read(position, buffer);

            assertThat(decode(buffer), is(i));

            position += recordLength + Record.HEADER_LENGTH;
        }
    }

    // TODO assert behaviour -> on end of page, writer should add next page marker, then try to claim from the next page



    @Test
    public void shouldIndicateThatPageFreeSpaceIsExhausted() throws Exception {
        final ByteBuffer data = ByteBuffer.allocate(5000);
        assertThat(page.write(data), is(WriteResult.SUCCESS));
        data.clear();
        assertThat(page.write(data), is(WriteResult.NOT_ENOUGH_SPACE));
    }

    private static long decode(final ByteBuffer source) {
        return source.getLong();
    }

    private static void encode(final long payload, final ByteBuffer target) {
        target.clear();
        target.putLong(payload);
        target.flip();
    }
}