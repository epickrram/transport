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
package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.threads.SingleThreaded;

import java.nio.ByteBuffer;

@SingleThreaded
public final class WritableRecord
{
    private ByteBuffer buffer;
    private Page page;
    private int headerOffset;
    private int recordLength;

    public ByteBuffer buffer()
    {
        return buffer;
    }

    public void commit()
    {
        page.writeReadyHeader(headerOffset, recordLength);
        page.releaseReference();
    }

    void set(final ByteBuffer buffer, final Page page, final int headerOffset)
    {
        this.buffer = buffer;
        this.recordLength = buffer.remaining();
        this.page = page;
        this.headerOffset = headerOffset;
    }
}