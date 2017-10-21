package com.aitusoftware.transport.messaging;

@Message
public interface ExecutionReport
{
    long timestamp();
    CharSequence statusMessage();
    boolean isBid();
    CharSequence orderId();
    double quantity();
    double price();
}
