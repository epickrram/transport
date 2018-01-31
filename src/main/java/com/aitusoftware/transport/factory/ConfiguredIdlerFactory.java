package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.messaging.Topic;
import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;

import java.util.function.Function;

public final class ConfiguredIdlerFactory implements Function<Class<?>, Idler>
{
    private final String prefix;
    private final Function<String, String> keyValueMapper;
    private final Idler fallbackIdler;

    public ConfiguredIdlerFactory(
            final String prefix, final Function<String, String> keyValueMapper,
            final Idler fallbackIdler)
    {
        this.prefix = prefix;
        this.keyValueMapper = keyValueMapper;
        this.fallbackIdler = fallbackIdler;
    }

    @Override
    public Idler apply(final Class<?> topicClass)
    {
        if (topicClass.getAnnotation(Topic.class) == null)
        {
            throw new IllegalArgumentException(String.format(
                    "Not a topic spec: %s", topicClass.getName()));
        }

        final String property = keyValueMapper.apply(prefix + topicClass.getName());
        if (property == null)
        {
            return fallbackIdler;
        }
        return Idlers.forString(property);
    }
}
