package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.messaging.TopicDispatcherRecordHandler;
import com.aitusoftware.transport.messaging.TopicIdCalculator;
import com.aitusoftware.transport.messaging.proxy.PublisherFactory;
import com.aitusoftware.transport.messaging.proxy.Subscriber;
import com.aitusoftware.transport.messaging.proxy.SubscriberFactory;
import com.aitusoftware.transport.net.OutputChannel;
import com.aitusoftware.transport.reader.StreamingReader;
import org.agrona.collections.Int2ObjectHashMap;

import java.nio.file.Path;

public final class ServiceFactory
{
    private static final String PUBLISHER_PAGE_CACHE_PATH = "pub";
    private static final String SUBSCRIBER_PAGE_CACHE_PATH = "sub";

    private final Path pageCachePath;
    private final PublisherFactory publisherFactory;
    private final PageCache subscriberPageCache;
    private final Int2ObjectHashMap<Subscriber> topicToSubscriber = new Int2ObjectHashMap<>();
    private final SubscriberFactory subscriberFactory;
    private final PageCache publisherPageCache;

    public ServiceFactory(final Path pageCachePath)
    {
        this.pageCachePath = pageCachePath;
        publisherPageCache = PageCache.create(pageCachePath.resolve(PUBLISHER_PAGE_CACHE_PATH), 1 << 18);
        subscriberPageCache = PageCache.create(pageCachePath.resolve(SUBSCRIBER_PAGE_CACHE_PATH), 1 << 18);
        publisherFactory = new PublisherFactory(publisherPageCache);
        subscriberFactory = new SubscriberFactory();
    }

    public <T> T createPublisher(final Class<T> topicDefinition)
    {
        return publisherFactory.getPublisherProxy(topicDefinition);
    }

    public <T> void registerSubscriber(final SubscriberDefinition<T> definition)
    {
        final int topicId = TopicIdCalculator.calculate(definition.getTopic());
        topicToSubscriber.put(topicId, subscriberFactory.getSubscriber(definition.getTopic(), definition.getImplementation()));
    }

    public Service create()
    {
        final TopicDispatcherRecordHandler topicDispatcher =
                new TopicDispatcherRecordHandler(topicToSubscriber);

        final StreamingReader inboundReader =
                new StreamingReader(subscriberPageCache, topicDispatcher, true, true);
        final StreamingReader outboundReader =
                new StreamingReader(publisherPageCache, new OutputChannel(), true, true);
        return new Service(inboundReader, outboundReader);
    }
}
