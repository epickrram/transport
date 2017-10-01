package com.aitusoftware.transport.messaging.proxy;

final class MethodDescriptor
{
    private final int index;
    private final String name;
    private final ParameterDescriptor[] parameterTypes;

    MethodDescriptor(final int index, final String name, final ParameterDescriptor[] parameterTypes)
    {
        this.index = index;
        this.name = name;
        this.parameterTypes = parameterTypes;
    }

    int getIndex()
    {
        return index;
    }

    String getName()
    {
        return name;
    }

    ParameterDescriptor[] getParameterTypes()
    {
        return parameterTypes;
    }
}