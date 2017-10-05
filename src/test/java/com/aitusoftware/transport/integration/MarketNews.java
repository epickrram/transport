package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.messaging.Topic;

@Topic(listenAddress = "127.0.0.1", port = 12002)
public interface MarketNews
{
    void onNewsItem(final CharSequence symbol, final CharSequence note, final float severity);
}