package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.transport.messaging.Sized;

import java.nio.ByteBuffer;

public final class CoderCommon
{
    private static final int BYTES_PER_CHAR = 2;
    private static final int BYTES_PER_LENGTH = 4;

    public static <T> int getSerialisedMessageByteLength(final T message)
    {
        if (message instanceof Sized)
        {
            return ((Sized) message).length();
        }
        throw new IllegalStateException("Not a Sized instance: " + message);
    }

    public static int getSerialisedCharSequenceByteLength(final CharSequence charSequence)
    {
        return getCharSequenceByteLength(charSequence) + getLengthByteLength();
    }

    public static int getCharSequenceByteLength(final CharSequence charSequence)
    {
        return charSequence.length() * BYTES_PER_CHAR;
    }

    public static int getLengthByteLength()
    {
        return BYTES_PER_LENGTH;
    }

    public static int getCharSequenceLengthAtOffset(final ByteBuffer buffer, final int position)
    {
        return buffer.getInt(position) * BYTES_PER_CHAR;
    }

    public static int getSerialisedCharSequenceLengthAtOffset(final ByteBuffer buffer, final int position)
    {
        return (buffer.getInt(position) * BYTES_PER_CHAR) + getLengthByteLength();
    }
}