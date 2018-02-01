package com.aitusoftware.transport.net;

import com.aitusoftware.transport.threads.SingleThreaded;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;

@SingleThreaded
public final class SingleChannelTopicMessageHandler implements TopicMessageHandler
{
    private final ByteBuffer[] srcs = new ByteBuffer[2];
    private final TopicToChannelMapper channelMapper;
    private final ByteBuffer lengthBuffer;


    public SingleChannelTopicMessageHandler(
            final TopicToChannelMapper channelMapper)
    {
        this.channelMapper = channelMapper;
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

        do
        {
            try
            {
                final GatheringByteChannel channel = channelMapper.forTopic(topicId);
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
                channelMapper.reconnectChannel(topicId);
            }
        }
        while ((data.remaining() != 0 || lengthBuffer.remaining() != 0) &&
                !Thread.currentThread().isInterrupted());
    }
}
