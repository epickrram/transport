package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.messaging.Topic;

@Topic(listenAddress = "127.0.0.1", port = 12004)
public interface TradeNotifications
{
    void onOrderAccepted(
            final CharSequence symbol, final CharSequence orderId,
            final boolean isBid, final long matchedQuantity,
            final long remainingQuantity, final double price,
            final int ecnId);

    void onOrderRejected(
            final CharSequence symbol, final CharSequence orderId,
            final int ecnId, final int rejectionReason);
}