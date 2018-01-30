package com.aitusoftware.transport.integration;

import com.aitusoftware.transport.net.AddressSpace;

final class DelegatingAddressSpace implements AddressSpace
{
    private final AddressSpace delegate;
    private final int traderBotListenPort;
    private final int orderGatewayListenPort;

    DelegatingAddressSpace(final AddressSpace delegate,
                           final int traderBotListenPort,
                           final int orderGatewayListenPort)
    {
        this.delegate = delegate;
        this.traderBotListenPort = traderBotListenPort;
        this.orderGatewayListenPort = orderGatewayListenPort;
    }

    @Override
    public int portOf(final Class<?> topicClass)
    {
        if (MarketData.class.isAssignableFrom(topicClass) ||
                MarketNews.class.isAssignableFrom(topicClass) ||
                TradeNotifications.class.isAssignableFrom(topicClass))
        {
            return traderBotListenPort;
        }
        return orderGatewayListenPort;
    }

    @Override
    public String hostOf(final Class<?> topicClass)
    {
        return delegate.hostOf(topicClass);
    }
}
