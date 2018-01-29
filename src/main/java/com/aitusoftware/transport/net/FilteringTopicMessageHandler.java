package com.aitusoftware.transport.net;

import java.nio.ByteBuffer;

public final class FilteringTopicMessageHandler implements TopicMessageHandler
{
    private final int topicId;
    private final TopicMessageHandler delegate;

    private FilteringTopicMessageHandler(final int topicId, final TopicMessageHandler delegate)
    {
        this.topicId = topicId;
        this.delegate = delegate;
    }

    @Override
    public void onTopicMessage(final int topicId, final ByteBuffer data)
    {
        if (topicId == this.topicId)
        {
            delegate.onTopicMessage(topicId, data);
        }
    }

    public static TopicMessageHandler filter(final int topicId, final TopicMessageHandler delegate)
    {
        return new FilteringTopicMessageHandler(topicId, delegate);
    }
}