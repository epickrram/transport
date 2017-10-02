package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.buffer.WritableRecord;
import com.aitusoftware.transport.messaging.TopicIdCalculator;

public abstract class AbstractPublisher
{
    private final PageCache pageCache;
    private final int topicId;

    protected AbstractPublisher(final PageCache pageCache)
    {
        this.pageCache = pageCache;
        this.topicId = TopicIdCalculator.calculate(getClass());
    }

    protected WritableRecord acquireRecord(final int recordLength, final byte methodId)
    {
        final WritableRecord record = pageCache.acquireRecordBuffer(recordLength + 5);
        record.buffer().putInt(topicId);
        record.buffer().put(methodId);
        return record;
    }
}