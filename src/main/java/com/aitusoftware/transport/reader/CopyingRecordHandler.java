package com.aitusoftware.transport.reader;

import com.aitusoftware.transport.buffer.PageCache;

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

    }
}