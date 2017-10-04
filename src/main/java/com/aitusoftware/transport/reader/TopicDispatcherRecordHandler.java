package com.aitusoftware.transport.reader;

import com.aitusoftware.transport.messaging.proxy.AbstractSubscriber;
import org.agrona.collections.Int2ObjectHashMap;

import java.nio.ByteBuffer;

public final class TopicDispatcherRecordHandler implements RecordHandler
{
    private final Int2ObjectHashMap<AbstractSubscriber<?>> topicIdToSubscriberMap;

    public TopicDispatcherRecordHandler(final Int2ObjectHashMap<AbstractSubscriber<?>> topicIdToSubscriberMap)
    {
        this.topicIdToSubscriberMap = topicIdToSubscriberMap;
    }

    @Override
    public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
    {
        final int topicId = data.getInt();
        final AbstractSubscriber<?> subscriber = topicIdToSubscriberMap.get(topicId);
        subscriber.onRecord(data, pageNumber, position);
    }
}
