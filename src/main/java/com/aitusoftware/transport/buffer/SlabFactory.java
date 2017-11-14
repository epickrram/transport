package com.aitusoftware.transport.buffer;

import java.nio.ByteBuffer;

public final class SlabFactory
{
    private SlabFactory()
    {
    }

    public static Slab createSlab(final int size)
    {
        return new Slab(ByteBuffer.allocateDirect(size));
    }

    public static Slab createSlab(final ByteBuffer buffer)
    {
        return new Slab(buffer);
    }
}
