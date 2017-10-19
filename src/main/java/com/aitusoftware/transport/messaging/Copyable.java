package com.aitusoftware.transport.messaging;

public interface Copyable<T>
{
    default T heapCopy()
    {
        throw new UnsupportedOperationException();
    }
}
