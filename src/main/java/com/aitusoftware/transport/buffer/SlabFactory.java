package com.aitusoftware.transport.buffer;

import java.nio.ByteBuffer;

public enum SlabFactory
{
    SLAB_FACTORY;

    public Slab createSlab(final int size)
    {
        return new Slab(ByteBuffer.allocateDirect(size));
    }

    public Slab createSlab(final ByteBuffer buffer)
    {
        return new Slab(buffer);
    }
}
