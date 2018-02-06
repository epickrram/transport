/*
 * Copyright 2017 - 2018 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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