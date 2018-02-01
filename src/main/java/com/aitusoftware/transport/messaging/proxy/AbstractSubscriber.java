package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.transport.messaging.TopicIdCalculator;
import com.aitusoftware.transport.threads.SingleThreaded;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractSubscriber<T> implements Subscriber<T>
{
    private final T implementation;
    private final MethodInvoker<T>[] invokers;
    private final int topicId;
    private final AtomicLong messageCount = new AtomicLong();
    private long localMessageCount;

    protected AbstractSubscriber(final T implementation, final MethodInvoker<T>[] invokers)
    {
        this.implementation = implementation;
        this.invokers = invokers;
        topicId = TopicIdCalculator.calculate(implementation.getClass());
    }

    @SingleThreaded
    @Override
    public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
    {
        localMessageCount++;
        messageCount.lazySet(localMessageCount);
        final byte methodIndex = data.get();
        invokers[methodIndex].invoke(implementation, data);
    }

    @Override
    public int getTopicId()
    {
        return topicId;
    }

    @Override
    public long getMessageCount()
    {
        return messageCount.get();
    }
}