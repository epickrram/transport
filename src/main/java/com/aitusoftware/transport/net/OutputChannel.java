package com.aitusoftware.transport.net;

import com.aitusoftware.transport.reader.RecordHandler;
import org.agrona.collections.Int2ObjectHashMap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public final class OutputChannel implements RecordHandler
{
    private final TopicToChannelMapper channelMapper;
    private final Int2ObjectHashMap<ByteBuffer> enqueuedData = new Int2ObjectHashMap<>();

    public OutputChannel(final TopicToChannelMapper channelMapper)
    {
        this.channelMapper = channelMapper;
    }

    @Override
    public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
    {
        final int topicId = data.getInt(data.position());

        final SocketChannel channel = channelMapper.forTopic(topicId);
        while (data.remaining() != 0)
        {
            try
            {
                channel.write(data);
            }
            catch (IOException e)
            {
                // TODO buffer data
                channelMapper.reconnectChannel(topicId);
            }
        }
    }
}
