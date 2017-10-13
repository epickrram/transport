package com.aitusoftware.transport.reader;

import com.aitusoftware.transport.buffer.Offsets;
import com.aitusoftware.transport.buffer.Page;
import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.PausingIdler;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class StreamingReader
{
    private final PageCache pageCache;
    private final RecordHandler recordHandler;
    private final boolean tail;
    private final boolean zeroCopy;
    private final Idler idler = new PausingIdler(1, TimeUnit.MILLISECONDS);
    private final AtomicLong messageCount = new AtomicLong();
    private long localMessageCount;
    private int pageNumber = 0;
    private int position = 0;
    private Page page;
    private ByteBuffer buffer = ByteBuffer.allocateDirect(256);

    public StreamingReader(
            final PageCache pageCache, final RecordHandler recordHandler,
            final boolean tail, final boolean zeroCopy)
    {
        this.pageCache = pageCache;
        this.recordHandler = recordHandler;
        this.tail = tail;
        this.zeroCopy = zeroCopy;
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
                idler.idle();
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

            if (zeroCopy)
            {
                final ByteBuffer slice = pageCache.slice(pageNumber, position, recordLength);
                recordHandler.onRecord(slice, pageNumber, position);
            }
            else
            {
                if (recordLength > buffer.capacity())
                {
                    buffer = ByteBuffer.allocateDirect(toNextPowerOfTwo(recordLength));
                }
                buffer.clear();
                buffer.limit(recordLength);
                pageCache.read(pageNumber, position, buffer);
                recordHandler.onRecord(buffer, pageNumber, position);
            }
            localMessageCount++;
            messageCount.lazySet(localMessageCount);
            position += recordLength;
            position = Offsets.getAlignedPosition(position);
            if (position >= pageCache.getPageSize())
            {
                advancePage();
            }
            return true;
        }
        else if(Page.isEof(header))
        {
            advancePage();
        }
        else if(!tail)
        {
            return false;
        }
        return pageCache.isPageAvailable(pageNumber);
    }

    private void advancePage()
    {
        page = null;
        pageNumber++;
        position = 0;
    }

    public long getMessageCount()
    {
        return messageCount.get();
    }

    private static int toNextPowerOfTwo(final int input)
    {
        return Integer.highestOneBit(input) * 2;
    }
}