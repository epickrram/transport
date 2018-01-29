package com.aitusoftware.transport.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;

public final class MultiChannelTopicMessageHandler implements TopicMessageHandler
{
    private final ByteBuffer[] srcs = new ByteBuffer[2];
    private final TopicToChannelMapper channelMapper;
    private final int numberOfConnections;
    private final ByteBuffer lengthBuffer;

    public MultiChannelTopicMessageHandler(
            final TopicToChannelMapper channelMapper, final int numberOfConnections)
    {
        this.channelMapper = channelMapper;
        this.numberOfConnections = numberOfConnections;
        lengthBuffer = ByteBuffer.allocateDirect(4);
        srcs[0] = lengthBuffer;
    }

    @Override
    public void onTopicMessage(final int topicId, final ByteBuffer data)
    {
        srcs[1] = data;
        lengthBuffer.clear();
        // TODO should be able to determine length from record header
        lengthBuffer.putInt(0, data.remaining());
        for (int i = 0; i < numberOfConnections; i++)
        {
            lengthBuffer.clear();
            data.mark();
            writeToChannel(data, i);

            data.reset();
        }
    }

    private void writeToChannel(final ByteBuffer data, final int index)
    {
        while ((data.remaining() != 0 || lengthBuffer.remaining() != 0) &&
                !Thread.currentThread().isInterrupted())
        {
            try
            {
                final GatheringByteChannel channel = channelMapper.forTopic(index);
                if (channel == null)
                {
                    // TODO should be handled by reconnect logic
                    return;
                }
                channel.write(srcs);
            }
            catch (RuntimeException | IOException e)
            {
                // TODO buffer data
                channelMapper.reconnectChannel(index);
                return;
            }
        }
    }
}
