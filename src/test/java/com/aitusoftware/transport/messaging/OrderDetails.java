package com.aitusoftware.transport.messaging;

@Message
public interface OrderDetails extends Copyable<OrderDetails>
{
    long orderId();
    double quantity();
    double price();
    CharSequence getIdentifier();
}