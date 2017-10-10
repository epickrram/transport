package com.aitusoftware.transport.integration;

import java.util.concurrent.CountDownLatch;

public final class TraderBot implements MarketData, MarketNews, TradeNotifications
{
    private final OrderNotifications orderNotifications;
    private final CountDownLatch orderAcceptedLatch = new CountDownLatch(1);
    private int updateCount = 0;

    TraderBot(final OrderNotifications orderNotifications)
    {
        this.orderNotifications = orderNotifications;
    }

    @Override
    public void onAsk(final CharSequence symbol, final long quantity, final double price, final int sourceId)
    {
        System.out.println("onAsk");
        if (updateCount++ > 10)
        {
            System.out.println("send limitOrder");
            orderNotifications.limitOrder(symbol, "order-" + updateCount, true, 17L, 54.5d, 11);
            updateCount = 0;
        }
    }

    @Override
    public void onBid(final CharSequence symbol, final long quantity, final double price, final int sourceId)
    {
        if (updateCount++ > 10)
        {
            orderNotifications.limitOrder(symbol, "order-" + updateCount, false, 17L, 54.5d, 11);
            updateCount = 0;
        }
    }

    @Override
    public void onTrade(final CharSequence symbol, final boolean isBuy, final long quantity, final double price, final int sourceId)
    {
        updateCount++;
    }

    @Override
    public void onNewsItem(final CharSequence symbol, final CharSequence note, final float severity)
    {
        updateCount++;
    }

    @Override
    public void onOrderAccepted(final CharSequence symbol, final CharSequence orderId, final boolean isBid, final long matchedQuantity,
                                final long remainingQuantity, final double price, final int ecnId)
    {
        orderAcceptedLatch.countDown();
    }

    @Override
    public void onOrderRejected(final CharSequence symbol, final CharSequence orderId, final int ecnId, final int rejectionReason)
    {
    }

    CountDownLatch getOrderAcceptedLatch()
    {
        return orderAcceptedLatch;
    }
}