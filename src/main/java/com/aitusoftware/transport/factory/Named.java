package com.aitusoftware.transport.factory;

final class Named<T>
{
    private final String name;
    private final T value;

    private Named(final String name, final T value)
    {
        this.name = name;
        this.value = value;
    }

    static <T> Named<T> named(final String named, final T value)
    {
        return new Named<>(named, value);
    }

    String name()
    {
        return name;
    }

    T value()
    {
        return value;
    }
}
