package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.threads.SingleThreaded;

import java.nio.ByteBuffer;

@SingleThreaded
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
