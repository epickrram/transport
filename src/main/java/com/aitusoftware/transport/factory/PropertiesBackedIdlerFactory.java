package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.messaging.Topic;
import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;

import java.util.Properties;
import java.util.function.Function;

public final class PropertiesBackedIdlerFactory implements Function<Class<?>, Idler>
{
    private final Properties properties;
    private final Idler fallbackIdler;

    public PropertiesBackedIdlerFactory(
            final Properties properties,
            final Idler fallbackIdler)
    {
        this.properties = properties;
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

        final String property = properties.getProperty(topicClass.getName());
        if (property == null)
        {
            return fallbackIdler;
        }
        return Idlers.forString(property);
    }
}
