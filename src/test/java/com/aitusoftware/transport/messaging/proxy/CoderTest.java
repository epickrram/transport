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