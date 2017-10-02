package com.aitusoftware.transport.reader;

import com.aitusoftware.transport.buffer.Fixtures;
import com.aitusoftware.transport.buffer.PageCache;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StreamingReaderTest
{
    private static final int MESSAGE_COUNT = 500;
    private final ByteBuffer message = ByteBuffer.allocate(337);
    private final PageCache pageCache = PageCache.create(Fixtures.tempDirectory(), 4096);
    private final CapturingRecordHandler handler = new CapturingRecordHandler();

    @Test
    public void shouldReadAllEntriesWithBufferCopy() throws Exception
    {
        Fixtures.writeMessages(message, pageCache, MESSAGE_COUNT);

        createReader(false).process();

        assertThat(handler.messageCount, is(MESSAGE_COUNT));
    }

    @Test
    public void shouldReadAllEntriesZeroCopy() throws Exception
    {
        Fixtures.writeMessages(message, pageCache, MESSAGE_COUNT);

        createReader(true).process();

        assertThat(handler.messageCount, is(MESSAGE_COUNT));
    }

    private StreamingReader createReader(final boolean zeroCopy)
    {
        return new StreamingReader(pageCache, handler, false, zeroCopy);
    }

    private static final class CapturingRecordHandler implements RecordHandler
    {
        private int messageCount;

        @Override
        public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
        {
            final byte[] copy = new byte[data.remaining()];
            data.mark();
            data.get(copy);
            data.reset();

            assertTrue("Bad message at: " + messageCount + "/" + new String(copy),
                    Fixtures.isValidMessage(data, messageCount));
            messageCount++;
        }
    }

}