package com.aitusoftware.transport.messaging.proxy;

final class ParameterDescriptor
{
    private final String name;
    private final Class<?> type;

    ParameterDescriptor(final String name, final Class<?> type)
    {
        this.name = name;
        this.type = type;
    }

    String getName()
    {
        return name;
    }

    Class<?> getType()
    {
        return type;
    }
}