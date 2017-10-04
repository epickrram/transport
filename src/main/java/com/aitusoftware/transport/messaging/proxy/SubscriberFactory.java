package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.proxygen.Constants;

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