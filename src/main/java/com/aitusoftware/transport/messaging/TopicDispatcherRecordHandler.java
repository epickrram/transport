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
package com.aitusoftware.transport.messaging;

import com.aitusoftware.transport.messaging.proxy.Subscriber;
import com.aitusoftware.transport.reader.RecordHandler;
import org.agrona.collections.Int2ObjectHashMap;

import java.nio.ByteBuffer;

public final class TopicDispatcherRecordHandler implements RecordHandler
{
    private final Int2ObjectHashMap<Subscriber> topicIdToSubscriberMap;

    public TopicDispatcherRecordHandler(final Int2ObjectHashMap<Subscriber> topicIdToSubscriberMap)
    {
        this.topicIdToSubscriberMap = topicIdToSubscriberMap;
    }

    @Override
    public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
    {
        final int topicId = data.getInt();
        final Subscriber subscriber = topicIdToSubscriberMap.get(topicId);
        subscriber.onRecord(data, pageNumber, position);
    }
}
