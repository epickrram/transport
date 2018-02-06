/*
 * Copyright 2017 - 2018 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
