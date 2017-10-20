package com.aitusoftware.transport.messaging;

@Topic
public interface MessageTestTopic
{
    void testCompositeMessageArgument(final long id, final OrderDetails orderDetails);
}
