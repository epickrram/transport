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
import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;

import java.util.Properties;
import java.util.function.Function;

public final class ConfiguredIdlerFactory implements Function<Class<?>, Idler>
{
    private final String prefix;
    private final Function<String, String> keyValueMapper;
    private final Idler fallbackIdler;

    ConfiguredIdlerFactory(
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

    static ConfiguredIdlerFactory fromProperties(
            final Properties properties, final String prefix, final Idler fallbackIdler)
    {
        return new ConfiguredIdlerFactory(prefix, properties::getProperty, fallbackIdler);
    }
}
