/*
 * Copyright 2017 - 2018 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
            buffer.putChar(seq.charAt(i));
        }
    }
}
