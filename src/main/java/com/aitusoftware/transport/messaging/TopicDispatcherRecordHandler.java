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
