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

import com.aitusoftware.transport.buffer.Offsets;
import com.aitusoftware.transport.buffer.Page;
import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.buffer.Record;
import com.aitusoftware.transport.buffer.Slice;
import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.SingleThreaded;

import java.util.concurrent.atomic.AtomicLong;

public final class StreamingReader
{
    private final PageCache pageCache;
    private final RecordHandler recordHandler;
    private final boolean tail;
    private final Idler idler;
    private final AtomicLong messageCount = new AtomicLong();
    private long localMessageCount;
    private int pageNumber = 0;
    private int position = 0;
    private Page page;
    private StreamingReaderContext context;

    public StreamingReader(
            final PageCache pageCache, final RecordHandler recordHandler,
            final boolean tail, final Idler idler)
    {
        this.pageCache = pageCache;
        this.recordHandler = recordHandler;
        this.tail = tail;
        this.idler = idler;
    }

    @SingleThreaded
    public void process()
    {
        while (!Thread.currentThread().isInterrupted())
        {
            context = StreamingReaderContext.get();

            if (!processRecord())
            {
                if (!tail)
                {
                    return;
                }
                idler.idle();
            }
            else
            {
                idler.reset();
            }
        }
    }

    private boolean processRecord()
    {
        if (page == null)
        {
            if (!pageCache.isPageAvailable(pageNumber))
            {
                return false;
            }
            page = pageCache.getPage(pageNumber);
        }

        final int header = page.header(position);
        final int recordLength = Page.recordLength(header);
        if (Page.isReady(header))
        {
            final Slice slice = pageCache.slice(pageNumber, position, recordLength);
            try
            {
                context.update(pageNumber, position, localMessageCount);
                recordHandler.onRecord(slice.buffer(), pageNumber, position);
            }
            finally
            {
                context.reset();
                slice.release();
            }
            localMessageCount++;
            messageCount.lazySet(localMessageCount);
            position += recordLength + Record.HEADER_LENGTH;
            position = Offsets.getAlignedPosition(position);
            if (position >= pageCache.getPageSize())
            {
                advancePage();
            }
            return true;
        }
        else if (Page.isEof(header))
        {
            advancePage();
        }
        else if (!tail)
        {
            return false;
        }

        return pageCache.isPageAvailable(pageNumber);
    }

    private void advancePage()
    {
        if (page != null)
        {
            page.releaseReference();
        }
        page = null;
        pageNumber++;
        position = 0;
    }

    public long getMessageCount()
    {
        return messageCount.get();
    }
}