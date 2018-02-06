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
package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.proxygen.common.Constants;

import java.lang.reflect.InvocationTargetException;

public final class SubscriberFactory
{
    @SuppressWarnings("unchecked")
    public <T> Subscriber<T> getSubscriber(
            final Class<T> topicDefinition, final T implementation)
    {
        final String subscriberProxyClassname = topicDefinition.getName() +
                Constants.PROXYGEN_SUBSCRIBER_SUFFIX;

        try
        {
            final Class<T> proxyClass = (Class<T>) Class.forName(subscriberProxyClassname);
            return (AbstractSubscriber<T>) proxyClass.getDeclaredConstructor(topicDefinition).newInstance(implementation);
        }
        catch (ClassNotFoundException | IllegalAccessException |
                NoSuchMethodException | InstantiationException |
                InvocationTargetException e)
        {
            throw new IllegalArgumentException("Failed to load subscriber for " + topicDefinition.getName(), e);
        }
    }
}