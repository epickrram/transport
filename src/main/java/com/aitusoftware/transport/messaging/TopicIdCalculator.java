package com.aitusoftware.transport.messaging;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public final class TopicIdCalculator
{
    public static int calculate(final Class<?> topicDefinition)
    {
        final StringBuilder builder = new StringBuilder();
        final Class<?> topicInterface;
        if (topicDefinition.isInterface() && topicDefinition.getDeclaredAnnotation(Topic.class) != null)
        {
            topicInterface = topicDefinition;
        }
        else
        {
            final Class<?>[] interfaces = topicDefinition.getInterfaces();
            final Optional<Class<?>> first = Arrays.stream(interfaces).filter(
                    iface -> iface.getDeclaredAnnotation(Topic.class) != null).findFirst();

            topicInterface = first.orElseThrow(() -> new IllegalStateException("No interface declaring Topic annotation"));
        }
        builder.append(topicInterface.getName());
        final Method[] methods = topicInterface.getDeclaredMethods();
        Arrays.sort(methods, MethodComparator.INSTANCE);
        for (Method method : methods)
        {
            builder.append(method.toGenericString());
        }

        return builder.toString().hashCode();
    }

    private enum MethodComparator implements Comparator<Method>
    {
        INSTANCE;

        @Override
        public int compare(final Method o1, final Method o2)
        {
            return o1.toGenericString().compareTo(o2.toGenericString());
        }
    }
}
