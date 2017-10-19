package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.proxygen.common.Constants;
import com.aitusoftware.transport.buffer.PageCache;

import java.lang.reflect.InvocationTargetException;

public final class PublisherFactory
{
    private final PageCache pageCache;

    public PublisherFactory(final PageCache pageCache)
    {
        this.pageCache = pageCache;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPublisherProxy(final Class<T> topicDefinition)
    {
        final String publisherProxyClassname = topicDefinition.getName() +
                Constants.PROXYGEN_PUBLISHER_SUFFIX;

        try
        {
            final Class<T> proxyClass = (Class<T>) Class.forName(publisherProxyClassname);
            return proxyClass.getDeclaredConstructor(PageCache.class).newInstance(pageCache);
        }
        catch (ClassNotFoundException | IllegalAccessException |
                NoSuchMethodException | InstantiationException |
                InvocationTargetException e)
        {
            throw new IllegalArgumentException("Failed to load publisher for " + topicDefinition.getName(), e);
        }
    }
}