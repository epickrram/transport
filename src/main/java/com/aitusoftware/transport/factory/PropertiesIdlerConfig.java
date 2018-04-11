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

import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;

import java.util.Optional;
import java.util.Properties;

import static com.aitusoftware.transport.factory.ConfiguredIdlerFactory.fromProperties;

public final class PropertiesIdlerConfig implements IdlerConfig
{
    static final String INVOKER_IDLER_PROPERTY = "transport.app.idler.invoker";
    private final Properties properties;
    private final ConfiguredIdlerFactory subscriberIdlers;
    private final ConfiguredIdlerFactory publisherIdlers;
    private final Idler fallbackIdler;

    public PropertiesIdlerConfig(final Properties properties, final Idler fallbackIdler)
    {
        this.properties = properties;
        this.subscriberIdlers = fromProperties(properties, "transport.subscriber.idler.", fallbackIdler);
        this.publisherIdlers = fromProperties(properties, "transport.publisher.idler.", fallbackIdler);
        this.fallbackIdler = fallbackIdler;
    }

    @Override
    public Idler getInvokerIdler()
    {
        return Optional.ofNullable(properties.getProperty(INVOKER_IDLER_PROPERTY)).
                map(Idlers::forString).orElse(fallbackIdler);
    }

    @Override
    public Idler getPublisherIdler(final Class<?> topicDefinition)
    {
        return publisherIdlers.apply(topicDefinition);
    }

    @Override
    public Idler getSubscriberIdler(final Class<?> topicDefinition)
    {
        return subscriberIdlers.apply(topicDefinition);
    }
}