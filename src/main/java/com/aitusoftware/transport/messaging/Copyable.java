package com.aitusoftware.transport.messaging;

public interface Copyable<T>
{
    default T heapCopy()
    {
        throw new UnsupportedOperationException();
    }

    default void copyTo(final Builder<T> builder)
    {
        throw new UnsupportedOperationException();
    }
}
