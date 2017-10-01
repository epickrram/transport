package com.aitusoftware.transport.messaging.proxy;

import java.nio.ByteBuffer;

public final class Encoder
{
    public static void encodeBoolean(final ByteBuffer buffer, final boolean value)
    {
        buffer.put(value ? (byte) 1 : 0);
    }

    public static void encodeByte(final ByteBuffer buffer, final byte value)
    {
        buffer.put(value);
    }

    public static void encodeShort(final ByteBuffer buffer, final short value)
    {
        buffer.putShort(value);
    }

    public static void encodeInt(final ByteBuffer buffer, final int value)
    {
        buffer.putInt(value);
    }

    public static void encodeChar(final ByteBuffer buffer, final char value)
    {
        buffer.putInt(value);
    }

    public static void encodeFloat(final ByteBuffer buffer, final float value)
    {
        buffer.putInt(Float.floatToIntBits(value));
    }

    public static void encodeLong(final ByteBuffer buffer, final long value)
    {
        buffer.putLong(value);
    }

    public static void encodeDouble(final ByteBuffer buffer, final double value)
    {
        buffer.putLong(Double.doubleToLongBits(value));
    }

    public static void encodeCharSequence(final ByteBuffer buffer, final CharSequence seq)
    {
        buffer.putInt(seq.length());
        for (int i = 0; i < seq.length(); i++)
        {
            buffer.putInt(seq.charAt(i));
        }
    }
}
