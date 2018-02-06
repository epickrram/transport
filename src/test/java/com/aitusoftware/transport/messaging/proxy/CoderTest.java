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

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CoderTest
{
    private static final String IDENTIFIER = "identifier";
    private final ByteBuffer buffer = ByteBuffer.allocate(256);

    @Test
    public void shouldEncodeCharSequence() throws Exception
    {
        Encoder.encodeCharSequence(buffer, IDENTIFIER);
        buffer.flip();
        buffer.mark();
        final StringBuilder target = new StringBuilder();
        Decoder.decodeCharSequence(buffer, target);

        assertThat(target.toString(), is(IDENTIFIER));

        buffer.reset();
        target.setLength(0);

        Decoder.decodeCharSequenceAt(buffer, 0, target);

        assertThat(target.toString(), is(IDENTIFIER));
    }
}