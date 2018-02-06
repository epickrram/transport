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
package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.messaging.Topic;
import com.aitusoftware.transport.threads.Threads;

import java.util.function.Consumer;
import java.util.function.Function;

public final class ConfiguredAffinityWrapper
{
    private static final int NO_AFFINITY = -1;
    private final String prefix;
    private final Function<String, String> keyValueMapper;

    public ConfiguredAffinityWrapper(
            final String prefix, final Function<String, String> keyValueMapper)
    {
        this.prefix = prefix;
        this.keyValueMapper = keyValueMapper;
    }

    public Runnable wrap(
            final Runnable delegate, final Class<?> topicClass,
            final Consumer<Throwable> failureHandler)
    {
        if (topicClass.getAnnotation(Topic.class) == null)
        {
            throw new IllegalArgumentException(String.format(
                    "Not a topic spec: %s", topicClass.getName()));
        }

        final String value = keyValueMapper.apply(prefix + topicClass.getName());

        final int cpu = parseCpu(value);
        if (cpu != NO_AFFINITY)
        {
            return Threads.withAffinity(delegate, cpu, failureHandler);
        }
        failureHandler.accept(new IllegalArgumentException(String.format(
                "Not a valid cpu identifier: %s", value)));

        return delegate;
    }

    private static int parseCpu(final String value)
    {
        try
        {
            final int cpu = Integer.parseInt(value);
            return cpu < 0 ? NO_AFFINITY : cpu;
        }
        catch (IllegalArgumentException | NullPointerException e)
        {
            return NO_AFFINITY;
        }
    }
}