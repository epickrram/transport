package com.aitusoftware.transport.messaging.proxy;

import java.nio.ByteBuffer;

public interface MethodInvoker<T>
{
    void invoke(final T implementation, final ByteBuffer record);
}