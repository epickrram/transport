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

    private static final ThreadLocal<StringBuilder> CHAR_SEQUENCE =
            ThreadLocal.withInitial(StringBuilder::new);

    public static CharSequence decodeCharSequence(final ByteBuffer buffer)
    {
        final int length = buffer.getInt();
        final StringBuilder builder = CHAR_SEQUENCE.get();
        builder.setLength(0);
        for (int i = 0; i < length; i++)
        {
            builder.append(buffer.getChar());
        }

        return builder;
    }
}
