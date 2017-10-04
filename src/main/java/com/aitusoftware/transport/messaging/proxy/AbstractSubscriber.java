package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.transport.reader.RecordHandler;

import java.nio.ByteBuffer;

public abstract class AbstractSubscriber<T> implements RecordHandler
{
    private final T implementation;
    private final MethodInvoker<T>[] invokers;

    AbstractSubscriber(final T implementation, final MethodInvoker<T>[] invokers)
    {
        this.implementation = implementation;
        this.invokers = invokers;
    }

    @Override
    public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
    {
        // TODO should be used for dispatch to subscriber
        final int topicIdToBeConsumedByDispatcher = data.getInt();
        final byte methodIndex = data.get();
        invokers[methodIndex].invoke(implementation, data);
    }
}