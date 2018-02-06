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