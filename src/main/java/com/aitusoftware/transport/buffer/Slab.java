package com.aitusoftware.transport.buffer;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class Slab {
    private final ByteBuffer backingStore;
    private final VarHandle longArrayView;
    private final VarHandle intArrayView;

    public Slab(final ByteBuffer backingStore) {
        this.backingStore = backingStore;
        longArrayView = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.nativeOrder());
        intArrayView = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.nativeOrder());
    }

    public boolean compareAndSetLong(final int offset, final long expected, final long updated) {
        return longArrayView.compareAndSet(backingStore, offset, expected, updated);
    }

    public long getAndAddLong(final int offset, final long delta) {
        return (long) longArrayView.getAndAdd(backingStore, offset, delta);
    }

    public long getLongVolatile(final int offset) {
        return (long) longArrayView.getVolatile(backingStore, offset);
    }

    public boolean compareAndSetInt(final int offset, final int expected, final int updated) {
        return intArrayView.compareAndSet(backingStore, offset, expected, updated);
    }

    public int getAndAddInt(final int offset, final int delta) {
        return (int) intArrayView.getAndAdd(backingStore, offset, delta);
    }

    public int getIntVolatile(final int offset) {
        return (int) intArrayView.getVolatile(backingStore, offset);
    }

    public void writeOrderedInt(final int offset, final int value) {
        intArrayView.setRelease(backingStore, offset, value);
    }

    public void copy(final int offset, final ByteBuffer src) {
        final int startPosition = backingStore.position();
        backingStore.position(offset).put(src).position(startPosition);
    }

    public int capacity() {
        return backingStore.capacity();
    }
}