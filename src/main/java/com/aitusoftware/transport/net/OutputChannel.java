package com.aitusoftware.transport.net;

import com.aitusoftware.transport.reader.RecordHandler;

import java.nio.ByteBuffer;

public final class OutputChannel implements RecordHandler
{
    private final TopicMessageHandler messageHandler;

    public OutputChannel(final TopicMessageHandler messageHandler)
    {
        this.messageHandler = messageHandler;
    }

    @Override
    public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
    {
        final int topicId = data.getInt(data.position());
        messageHandler.onTopicMessage(topicId, data);
    }
}
