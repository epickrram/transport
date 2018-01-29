package com.aitusoftware.transport.net;

import java.nio.ByteBuffer;

public interface TopicMessageHandler
{
    void onTopicMessage(final int topicId, final ByteBuffer data);
}
