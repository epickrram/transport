package com.aitusoftware.transport.net;

import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;

public interface ServerSocketFactory
{
    void registerTopicAddress(int topicId, SocketAddress socketAddress);

    ServerSocketChannel acquire(int topicId);
}
