package com.aitusoftware.transport.buffer;

import java.nio.ByteBuffer;

public final class Slice
{
    private ByteBuffer buffer;
    private Page page;

    void set(final ByteBuffer buffer, final Page page)
    {
        this.buffer = buffer;
        this.page = page;
    }

    public ByteBuffer buffer()
    {
        return buffer;
    }

    public void release()
    {
        page.releaseReference();
    }
}
