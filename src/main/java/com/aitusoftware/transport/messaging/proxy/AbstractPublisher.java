package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.buffer.WritableRecord;

public abstract class AbstractPublisher
{
    private final PageCache pageCache;

    protected AbstractPublisher(final PageCache pageCache)
    {
        this.pageCache = pageCache;
    }

    protected WritableRecord acquireRecord(final int recordLength)
    {
        return pageCache.acquireRecordBuffer(recordLength);
    }


}