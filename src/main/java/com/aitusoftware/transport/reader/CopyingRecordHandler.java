package com.aitusoftware.transport.reader;

import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.buffer.WritableRecord;

import java.nio.ByteBuffer;

public final class CopyingRecordHandler implements RecordHandler
{
    private final PageCache pageCache;

    public CopyingRecordHandler(final PageCache pageCache)
    {
        this.pageCache = pageCache;
    }

    @Override
    public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
    {
        final WritableRecord writableRecord =
                pageCache.acquireRecordBuffer(data.remaining());
        try
        {
            writableRecord.buffer().put(data);
        }
        finally
        {
            writableRecord.commit();
        }
    }
}