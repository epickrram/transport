package com.aitusoftware.transport.reader;

import com.aitusoftware.transport.Fixtures;
import com.aitusoftware.transport.buffer.PageCache;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

public class CopyingRecordHandlerTest
{
    private static final byte[] PAYLOAD = "Something profound".getBytes(StandardCharsets.UTF_8);
    private CopyingRecordHandler handler;
    private PageCache pageCache;

    @Before
    public void setUp() throws Exception
    {
        pageCache = PageCache.create(Fixtures.tempDirectory(), 4096);
        handler = new CopyingRecordHandler(pageCache);
    }

    @Test
    public void shouldCopyRecord()
    {
        handler.onRecord(ByteBuffer.wrap(PAYLOAD), 0, 0);

        final ValidatingRecordHandler validator = new ValidatingRecordHandler();
        new StreamingReader(pageCache, validator, false, Fixtures.testIdler()).process();

        assertThat(validator.messageCount, is(1));
    }

    private static final class ValidatingRecordHandler implements RecordHandler
    {
        private int messageCount;

        @Override
        public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
        {
            assertThat(data.remaining(), is(PAYLOAD.length));
            final byte[] received = new byte[PAYLOAD.length];
            data.get(received);
            assertArrayEquals(PAYLOAD, received);
            messageCount++;
        }
    }
}