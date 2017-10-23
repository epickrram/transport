package com.aitusoftware.transport.messaging;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class MessageTest
{
    private static final long ORDER_ID = 17L;
    private static final double PRICE = 3.14;
    private static final double QUANTITY = Double.MAX_VALUE;
    private static final String IDENTIFIER = "identifier";

    @Test
    public void shouldBuildSerialiseAndCopy() throws Exception
    {
        final OrderDetailsBuilder details =
                new OrderDetailsBuilder().orderId(ORDER_ID).price(PRICE).
                        quantity(QUANTITY).setIdentifier(IDENTIFIER);

        assertProperties(details);
        final ByteBuffer buffer = ByteBuffer.allocate(details.length());

        new OrderDetailsSerialiser().serialise(details, buffer);
        buffer.flip();

        final OrderDetailsFlyweight flyweight = new OrderDetailsFlyweight();
        flyweight.reset(buffer);

        assertProperties(flyweight);
    }

    @Test
    public void shouldProvideHeapCopyFromFlyweight() throws Exception
    {
        final OrderDetailsBuilder details =
                new OrderDetailsBuilder().orderId(ORDER_ID).price(PRICE).
                        quantity(QUANTITY).setIdentifier(IDENTIFIER);

        final ByteBuffer buffer = ByteBuffer.allocate(details.length());

        OrderDetailsSerialiser.serialise(details, buffer);
        buffer.flip();

        final OrderDetailsFlyweight flyweight = new OrderDetailsFlyweight();
        flyweight.reset(buffer);

        assertProperties(flyweight.heapCopy());
    }

    @Test
    public void shouldEncodeBooleanFields() throws Exception
    {
        assertBooleanEncoding(true);
        assertBooleanEncoding(false);
    }

    private void assertBooleanEncoding(final boolean booleanValue)
    {
        final ExecutionReportBuilder builder = new ExecutionReportBuilder();
        builder.isBid(booleanValue).price(1).quantity(2).timestamp(3).orderId("id").statusMessage("status");

        final ByteBuffer buffer = ByteBuffer.allocate(builder.length());
        ExecutionReportSerialiser.serialise(builder, buffer);
        buffer.flip();

        final ExecutionReportFlyweight flyweight = new ExecutionReportFlyweight();
        flyweight.reset(buffer);

        assertThat(flyweight.isBid(), is(booleanValue));
    }

    private static void assertProperties(final OrderDetails details)
    {
        assertThat(details.orderId(), is(ORDER_ID));
        assertThat(details.quantity(), is(QUANTITY));
        assertThat(details.price(), is(PRICE));
        assertThat(IDENTIFIER + "!=" + details.getIdentifier(),
                IDENTIFIER.contentEquals(details.getIdentifier()), is(true));
    }
}