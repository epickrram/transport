package com.aitusoftware.transport.net;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

public interface AddressSpace
{
    int portOf(final Class<?> topicClass);
    String hostOf(final Class<?> topicClass);
    default List<SocketAddress> addressesOf(final Class<?> topicClass)
    {
        return Collections.singletonList(addressOf(topicClass));
    }

    default SocketAddress addressOf(final Class<?> topicClass)
    {
        final String hostname = hostOf(topicClass);
        final String hostAddress = "0.0.0.0".equals(hostname) ? "127.0.0.1" : hostname;
        return new InetSocketAddress(hostAddress, portOf(topicClass));
    }
}
