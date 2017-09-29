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
    private final StreamingReader reader = new StreamingReader(pageCache, handler, false);

    @Test
    public void shouldReadAllEntries() throws Exception
    {
        Fixtures.writeMessages(message, pageCache, MESSAGE_COUNT);

        reader.process();

        assertThat(handler.messageCount, is(MESSAGE_COUNT));
    }

    private static final class CapturingRecordHandler implements RecordHandler
    {
        private int messageCount;

        @Override
        public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
        {
            assertTrue(Fixtures.isValidMessage(data, messageCount));
            messageCount++;
        }
    }

}