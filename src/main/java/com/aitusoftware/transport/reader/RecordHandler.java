package com.aitusoftware.transport.reader;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface RecordHandler
{
    void onRecord(final ByteBuffer data, final int pageNumber, final int position);
}
