package com.aitusoftware.transport.messaging;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

public final class TopicIdCalculator
{
    public static int calculate(final Class<?> topicDefinition)
    {
        final StringBuilder builder = new StringBuilder();

        final Class<?>[] interfaces = topicDefinition.getInterfaces();
        final Class<?> topicInterface = Arrays.stream(interfaces).filter(
                iface -> iface.getDeclaredAnnotation(Topic.class) != null).findFirst().
                orElseThrow(() -> new IllegalStateException("No interface declaring Topic annotation"));
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
