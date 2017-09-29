package com.aitusoftware.transport.reader;

import com.aitusoftware.transport.buffer.Page;
import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.buffer.PageHeader;

import java.nio.ByteBuffer;

public final class StreamingReader
{
    private final PageCache pageCache;
    private final RecordHandler recordHandler;
    private final boolean tail;
    private int pageNumber = 0;
    private int position = 0;
    private Page page;
    private ByteBuffer buffer = ByteBuffer.allocateDirect(256);

    public StreamingReader(
            final PageCache pageCache, final RecordHandler recordHandler,
            final boolean tail)
    {
        this.pageCache = pageCache;
        this.recordHandler = recordHandler;
        this.tail = tail;
    }

    public void process()
    {
        while (!Thread.currentThread().isInterrupted())
        {
            if (!processRecord())
            {
                if (!tail)
                {
                    return;
                }
                Thread.yield();
            }
        }
    }

    public boolean processRecord()
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
        if (Page.isReady(header))
        {
            final int recordLength = Page.recordLength(header);
            if (recordLength > buffer.capacity())
            {
                buffer = ByteBuffer.allocateDirect(toNextPowerOfTwo(recordLength));
            }
            buffer.clear();
            buffer.limit(recordLength);
            page.read(position, buffer);
            recordHandler.onRecord(buffer, pageNumber, position);
            position += recordLength;
            position = PageHeader.getAlignedPosition(position);
            return true;
        }
        page = null;
        pageNumber++;
        position = 0;
        return true;
    }

    private static int toNextPowerOfTwo(final int input)
    {
        return Integer.highestOneBit(input) * 2;
    }
}