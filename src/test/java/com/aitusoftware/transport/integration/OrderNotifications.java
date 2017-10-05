package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.messaging.Topic;

@Topic(listenAddress = "127.0.0.1", port = 12003)
public interface OrderNotifications
{
    void limitOrder(
            final CharSequence symbol, final CharSequence orderId,
            final boolean isBid, final long quantity,
            final double price, final int ecnId);
    void marketOrder(
            final CharSequence symbol, final CharSequence orderId,
            final boolean isBid, final long quantity,
            final int ecnId);
    void cancelOrder(
            final CharSequence orderId, final int ecnId);
}