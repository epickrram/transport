package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.proxygen.Constants;
import com.aitusoftware.transport.buffer.PageCache;

import java.lang.reflect.InvocationTargetException;

public final class SubscriberFactory
{
    @SuppressWarnings("unchecked")
    public <T> AbstractSubscriber<T> getSubscriber(
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
            throw new IllegalArgumentException("Failed to load publisher for " + topicDefinition.getName());
        }
    }
}