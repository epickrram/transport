package com.aitusoftware.transport.net;

import com.aitusoftware.transport.reader.RecordHandler;

import java.nio.ByteBuffer;

public final class OutputChannel implements RecordHandler
{
    @Override
    public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
    {
        final int topicId = data.getInt(data.position());


    }
}
