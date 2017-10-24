package com.aitusoftware.transport.buffer;


import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class Slab
{
    private final ByteBuffer backingStore;
    private final VarHandle longArrayView;
    private final VarHandle intArrayView;
    private final ThreadLocal<ByteBuffer> threadLocalSlice;

    public Slab(final ByteBuffer backingStore)
    {
        this.backingStore = backingStore;
        // TODO should be static final
        longArrayView = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.nativeOrder());
        intArrayView = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.nativeOrder());
        threadLocalSlice = ThreadLocal.withInitial(backingStore::slice);
    }

    public boolean compareAndSetLong(final int offset, final long expected, final long updated)
    {
        return longArrayView.compareAndSet(backingStore, offset, expected, updated);
    }

    public long getAndAddLong(final int offset, final long delta)
    {
        return (long) longArrayView.getAndAdd(backingStore, offset, delta);
    }

    public long getLongVolatile(final int offset)
    {
        return (long) longArrayView.getVolatile(backingStore, offset);
    }

    public boolean compareAndSetInt(final int offset, final int expected, final int updated)
    {
        return intArrayView.compareAndSet(backingStore, offset, expected, updated);
    }

    public int getAndAddInt(final int offset, final int delta)
    {
        return (int) intArrayView.getAndAdd(backingStore, offset, delta);
    }

    public int getIntVolatile(final int offset)
    {
        return (int) intArrayView.getVolatile(backingStore, offset);
    }

    public void writeOrderedInt(final int offset, final int value)
    {
        intArrayView.setRelease(backingStore, offset, value);
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
            final Class<?> cls = Class.forName("sun.nio.ch.DirectBuffer");
            final Method method = cls.getDeclaredMethod("cleaner");
            method.setAccessible(true);
            final Object cleaner = method.invoke(backingStore);
            final Class<?> cls2 = Class.forName("jdk.internal.ref.Cleaner");
            final Method m2 = cls2.getDeclaredMethod("clean");
            m2.invoke(cleaner);
        }
        catch (Throwable throwable)
        {
            throwable.printStackTrace();
        }
    }
}