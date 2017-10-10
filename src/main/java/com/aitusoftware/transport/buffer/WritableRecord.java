package com.aitusoftware.transport.buffer;

import java.nio.ByteBuffer;

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
    }

    void set(final ByteBuffer buffer, final Page page, final int headerOffset)
    {
        this.buffer = buffer;
        this.recordLength = buffer.remaining();
        this.page = page;
        this.headerOffset = headerOffset;
    }
}