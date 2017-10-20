package com.aitusoftware.transport.messaging.proxy;

import java.nio.ByteBuffer;

public final class Decoder
{
    public static boolean decodeBoolean(final ByteBuffer buffer)
    {
        return buffer.get() != 0;
    }

    public static byte decodeByte(final ByteBuffer buffer)
    {
        return buffer.get();
    }

    public static short decodeShort(final ByteBuffer buffer)
    {
        return buffer.getShort();
    }

    public static int decodeInt(final ByteBuffer buffer)
    {
        return buffer.getInt();
    }

    public static char decodeChar(final ByteBuffer buffer)
    {
        return buffer.getChar();
    }

    public static float decodeFloat(final ByteBuffer buffer)
    {
        return Float.intBitsToFloat(buffer.getInt());
    }

    public static long decodeLong(final ByteBuffer buffer)
    {
        return buffer.getLong();
    }

    public static double decodeDouble(final ByteBuffer buffer)
    {
        return Double.longBitsToDouble(buffer.getLong());
    }

    public static CharSequence decodeCharSequence(final ByteBuffer buffer, final StringBuilder builder)
    {
        final int length = buffer.getInt();
        builder.setLength(0);
        for (int i = 0; i < length; i++)
        {
            builder.append(buffer.getChar());
        }

        return builder;
    }

    public static boolean decodeBooleanAt(final ByteBuffer buffer, final int offset)
    {
        return buffer.get(offset) != 0;
    }

    public static byte decodeByteAt(final ByteBuffer buffer, final int offset)
    {
        return buffer.get(offset);
    }

    public static short decodeShortAt(final ByteBuffer buffer, final int offset)
    {
        return buffer.getShort(offset);
    }

    public static int decodeIntAt(final ByteBuffer buffer, final int offset)
    {
        return buffer.getInt(offset);
    }

    public static char decodeCharAt(final ByteBuffer buffer, final int offset)
    {
        return buffer.getChar(offset);
    }

    public static float decodeFloatAt(final ByteBuffer buffer, final int offset)
    {
        return Float.intBitsToFloat(buffer.getInt(offset));
    }

    public static long decodeLongAt(final ByteBuffer buffer, final int offset)
    {
        return buffer.getLong(offset);
    }

    public static double decodeDoubleAt(final ByteBuffer buffer, final int offset)
    {
        return Double.longBitsToDouble(buffer.getLong(offset));
    }

    public static CharSequence decodeCharSequenceAt(final ByteBuffer buffer, final int offset, final StringBuilder builder)
    {
        final int length = buffer.getInt(offset);
        builder.setLength(0);
        for (int i = 0; i < length; i++)
        {
            builder.append(buffer.getChar(offset + 4 + (i * 2)));
        }

        return builder;
    }
}