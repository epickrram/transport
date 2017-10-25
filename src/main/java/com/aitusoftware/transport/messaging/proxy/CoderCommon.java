package com.aitusoftware.transport.messaging.proxy;

import java.nio.ByteBuffer;

public final class CoderCommon
{
    private static final int BYTES_PER_CHAR = 2;

    public static int getCharSequenceByteLength(final CharSequence charSequence)
    {
        return charSequence.length() * BYTES_PER_CHAR;
    }

    public static int getLengthByteLength()
    {
        return 4;
    }

    public static int getCharSequenceLengthAtOffset(final ByteBuffer buffer, final int position)
    {
        return buffer.getInt(position) * BYTES_PER_CHAR;
    }
}