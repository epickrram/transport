package com.aitusoftware.transport.messaging;

import java.lang.reflect.Method;
import java.util.Arrays;

public final class TopicIdCalculator
{
    public static int calculate(final Class<?> topicDefinition)
    {
        final StringBuilder builder = new StringBuilder();

        builder.append(topicDefinition.getName());
        final Method[] methods = topicDefinition.getDeclaredMethods();
        Arrays.sort(methods);
        for (Method method : methods)
        {
            builder.append(method.toGenericString());
        }

        return builder.toString().hashCode();
    }
}
