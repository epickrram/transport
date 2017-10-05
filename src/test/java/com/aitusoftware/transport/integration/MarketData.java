package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.messaging.Topic;

@Topic(listenAddress = "127.0.0.1", port = 12001)
public interface MarketData
{
    void onAsk(final CharSequence symbol, final long quantity, final double price, final int sourceId);
    void onBid(final CharSequence symbol, final long quantity, final double price, final int sourceId);
    void onTrade(final CharSequence symbol, final boolean isBuy, final long quantity, final double price, final int sourceId);
}