package com.aitusoftware.transport.buffer;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

public final class Slab
{
    private static final MethodHandle UNMAP;
    private static final VarHandle LONG_ARRAY_VIEW = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.nativeOrder());
    private static final VarHandle INT_ARRAY_VIEW = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.nativeOrder());
    private final ByteBuffer backingStore;
    private final ThreadLocal<ByteBuffer> threadLocalSlice;

    static
    {
        try
        {
            final Class<?> fileChannelClass = Class.forName("sun.nio.ch.FileChannelImpl");
            final Method unmapMethod = fileChannelClass.getDeclaredMethod("unmap", MappedByteBuffer.class);
            unmapMethod.setAccessible(true);
            UNMAP = MethodHandles.lookup().unreflect(unmapMethod);
        }
        catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException e)
        {
            throw new IllegalStateException("Cannot acquire MethodHandle to unmap()", e);
        }
    }

    public Slab(final ByteBuffer backingStore)
    {
        this.backingStore = backingStore;
        threadLocalSlice = ThreadLocal.withInitial(backingStore::slice);
    }

    public boolean compareAndSetLong(final int offset, final long expected, final long updated)
    {
        return LONG_ARRAY_VIEW.compareAndSet(backingStore, offset, expected, updated);
    }

    public long getAndAddLong(final int offset, final long delta)
    {
        return (long) LONG_ARRAY_VIEW.getAndAdd(backingStore, offset, delta);
    }

    public long getLongVolatile(final int offset)
    {
        return (long) LONG_ARRAY_VIEW.getVolatile(backingStore, offset);
    }

    public boolean compareAndSetInt(final int offset, final int expected, final int updated)
    {
        return INT_ARRAY_VIEW.compareAndSet(backingStore, offset, expected, updated);
    }

    public int getAndAddInt(final int offset, final int delta)
    {
        return (int) INT_ARRAY_VIEW.getAndAdd(backingStore, offset, delta);
    }

    public int getIntVolatile(final int offset)
    {
        return (int) INT_ARRAY_VIEW.getVolatile(backingStore, offset);
    }

    public void writeOrderedInt(final int offset, final int value)
    {
        INT_ARRAY_VIEW.setRelease(backingStore, offset, value);
    }

    public void copy(final int offset, final ByteBuffer source)
    {
        final ByteBuffer slice = threadLocalSlice.get();
        slice.clear();
        slice.position(offset).put(source);
    }

    public void copyInto(final int offset, final ByteBuffer destination)
    {
        final ByteBuffer slice = threadLocalSlice.get();
        slice.clear();
        slice.position(offset);
        slice.limit(offset + destination.remaining());
        destination.put(slice);
        destination.flip();
    }

    ByteBuffer slice()
    {
        return backingStore.slice();
    }

    public int capacity()
    {
        return backingStore.capacity();
    }

    @Override
    public String toString()
    {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < 32; i++)
        {
            buffer.append(Long.toHexString(backingStore.getLong(i * 8))).append(' ');
        }
        return buffer.toString();
    }

    void unmap()
    {
        try
        {
            if (backingStore instanceof MappedByteBuffer)
            {
                UNMAP.invokeExact((MappedByteBuffer) backingStore);
            }
        }
        catch (Throwable e)
        {
            throw new IllegalArgumentException("Unable to invoke unmap method", e);
        }
    }
}