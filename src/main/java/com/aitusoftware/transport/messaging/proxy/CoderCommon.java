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