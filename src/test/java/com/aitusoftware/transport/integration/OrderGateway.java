package com.aitusoftware.transport.integration;

public final class OrderGateway implements OrderNotifications
{
    private final TradeNotifications tradeNotifications;

    public OrderGateway(final TradeNotifications tradeNotifications)
    {
        this.tradeNotifications = tradeNotifications;
    }

    @Override
    public void limitOrder(
            final CharSequence symbol, final CharSequence orderId,
            final boolean isBid, final long quantity, final double price, final int ecnId)
    {
        tradeNotifications.onOrderAccepted(symbol, orderId, isBid, quantity,
                0, price, ecnId);
    }

    @Override
    public void marketOrder(
            final CharSequence symbol, final CharSequence orderId,
            final boolean isBid, final long quantity, final int ecnId)
    {
        tradeNotifications.onOrderAccepted(symbol, orderId, isBid, quantity,
                0, Double.MIN_VALUE, ecnId);
    }

    @Override
    public void cancelOrder(final CharSequence orderId, final int ecnId)
    {
    }
}
