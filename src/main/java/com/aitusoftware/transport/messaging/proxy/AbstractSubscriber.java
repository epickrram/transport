package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.transport.messaging.TopicIdCalculator;

import java.nio.ByteBuffer;

public abstract class AbstractSubscriber<T> implements Subscriber<T>
{
    private final T implementation;
    private final MethodInvoker<T>[] invokers;
    private final int topicId;

    protected AbstractSubscriber(final T implementation, final MethodInvoker<T>[] invokers)
    {
        this.implementation = implementation;
        this.invokers = invokers;
        topicId = TopicIdCalculator.calculate(implementation.getClass());
    }

    @Override
    public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
    {
        final byte methodIndex = data.get();
        invokers[methodIndex].invoke(implementation, data);
    }

    @Override
    public int getTopicId()
    {
        return topicId;
    }
}