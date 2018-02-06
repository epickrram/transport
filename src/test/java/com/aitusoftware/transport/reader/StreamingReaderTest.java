/*
 * Copyright 2017 - 2018 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aitusoftware.transport.reader;

import com.aitusoftware.transport.Fixtures;
import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.threads.Idlers;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StreamingReaderTest
{
    private static final int MESSAGE_COUNT = 500;
    private final ByteBuffer message = ByteBuffer.allocate(337);
    private final CapturingRecordHandler handler = new CapturingRecordHandler();
    private PageCache pageCache;

    @Before
    public void setUp() throws Exception
    {
        pageCache = PageCache.create(Fixtures.tempDirectory(), 4096);
    }

    @Test
    public void shouldReadAllEntriesWithBufferCopy() throws Exception
    {
        Fixtures.writeMessages(message, pageCache, MESSAGE_COUNT);

        createReader().process();

        assertThat(handler.messageCount, is(MESSAGE_COUNT));
    }

    @Test
    public void shouldReadAllEntriesZeroCopy() throws Exception
    {
        Fixtures.writeMessages(message, pageCache, MESSAGE_COUNT);

        createReader().process();

        assertThat(handler.messageCount, is(MESSAGE_COUNT));
    }

    private StreamingReader createReader()
    {
        return new StreamingReader(pageCache, handler, false, Idlers.staticPause(1, TimeUnit.MILLISECONDS));
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