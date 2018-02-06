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

import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.buffer.WritableRecord;
import com.aitusoftware.transport.messaging.TopicIdCalculator;
import com.aitusoftware.transport.threads.SingleThreaded;

import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractPublisher
{
    private static final int TOPIC_MESSAGE_HEADER_SIZE = 5;
    private final PageCache pageCache;
    private final int topicId;
    private final AtomicLong messageCount = new AtomicLong();

    protected AbstractPublisher(final PageCache pageCache)
    {
        this.pageCache = pageCache;
        this.topicId = TopicIdCalculator.calculate(getClass());
    }

    @SingleThreaded
    protected WritableRecord acquireRecord(final int recordLength, final byte methodId)
    {
        messageCount.incrementAndGet();
        final WritableRecord record =
                pageCache.acquireRecordBuffer(recordLength + TOPIC_MESSAGE_HEADER_SIZE);
        record.buffer().putInt(topicId);
        record.buffer().put(methodId);
        return record;
    }

    public long getMessageCount()
    {
        return messageCount.get();
    }

    public int getTopicId()
    {
        return topicId;
    }
}