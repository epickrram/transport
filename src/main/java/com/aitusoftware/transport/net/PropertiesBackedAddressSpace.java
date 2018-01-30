package com.aitusoftware.transport.net;

import com.aitusoftware.transport.messaging.Topic;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.util.stream.Collectors.toList;

public final class PropertiesBackedAddressSpace implements AddressSpace
{
    private final Properties properties;

    public PropertiesBackedAddressSpace(final Properties properties)
    {
        this.properties = properties;
    }

    @Override
    public List<SocketAddress> addressesOf(final Class<?> topicClass)
    {
        if (topicClass.getAnnotation(Topic.class) == null)
        {
            throw new IllegalArgumentException(String.format(
                    "Not a topic spec: %s", topicClass.getName()));
        }
        final String addressSpec = properties.getProperty(topicClass.getName());
        if (addressSpec == null)
        {
            throw new IllegalArgumentException(String.format("No address spec for topic defined by %s",
                    topicClass.getName()));
        }

        final String[] addresses = addressSpec.split(",");

        return Arrays.stream(addresses).map(spec -> spec.split(":")).
                map(tokens -> new InetSocketAddress(tokens[0], Integer.parseInt(tokens[1]))).
                collect(toList());
    }

    @Override
    public int portOf(final Class<?> topicClass)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String hostOf(final Class<?> topicClass)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketAddress addressOf(final Class<?> topicClass)
    {
        final List<SocketAddress> addresses = addressesOf(topicClass);
        if (addresses.size() != 1)
        {
            throw new UnsupportedOperationException(String.format(
                    "Multiple addresses (%d) are specified for topic defined by %s",
                    addresses.size(), topicClass.getName()));
        }

        return addresses.get(0);
    }
}
