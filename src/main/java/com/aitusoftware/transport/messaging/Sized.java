package com.aitusoftware.transport.messaging;

public interface Sized
{
    default int length()
    {
        throw new UnsupportedOperationException();
    }
}
