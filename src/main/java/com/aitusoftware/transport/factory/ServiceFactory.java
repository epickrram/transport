package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.messaging.TopicDispatcherRecordHandler;
import com.aitusoftware.transport.messaging.TopicIdCalculator;
import com.aitusoftware.transport.messaging.proxy.PublisherFactory;
import com.aitusoftware.transport.messaging.proxy.Subscriber;
import com.aitusoftware.transport.messaging.proxy.SubscriberFactory;
import com.aitusoftware.transport.net.OutputChannel;
import com.aitusoftware.transport.net.TopicToChannelMapper;
import com.aitusoftware.transport.reader.StreamingReader;
import org.agrona.collections.Int2ObjectHashMap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.function.IntFunction;

public final class ServiceFactory
{
    public static final String PUBLISHER_PAGE_CACHE_PATH = "pub";
    public static final String SUBSCRIBER_PAGE_CACHE_PATH = "sub";
    public static final int PAGE_SIZE = 1 << 18;

    private final Path pageCachePath;
    private final PublisherFactory publisherFactory;
    private final PageCache subscriberPageCache;
    private final Int2ObjectHashMap<Subscriber> topicToSubscriber = new Int2ObjectHashMap<>();
    private final SubscriberFactory subscriberFactory;
    private final PageCache publisherPageCache;
    private final SocketMapper socketMapper = new SocketMapper();

    public ServiceFactory(final Path pageCachePath)
    {
        this.pageCachePath = pageCachePath;
        publisherPageCache = PageCache.create(pageCachePath.resolve(PUBLISHER_PAGE_CACHE_PATH), PAGE_SIZE);
        subscriberPageCache = PageCache.create(pageCachePath.resolve(SUBSCRIBER_PAGE_CACHE_PATH), PAGE_SIZE);
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

    public <T> void registerRemoteSubscriber(
            final T implementation, final SocketAddress socketAddress)
    {
        socketMapper.addAddress(TopicIdCalculator.calculate(implementation.getClass()), socketAddress);
    }

    public Service create()
    {
        final TopicDispatcherRecordHandler topicDispatcher =
                new TopicDispatcherRecordHandler(topicToSubscriber);

        final StreamingReader inboundReader =
                new StreamingReader(subscriberPageCache, topicDispatcher, true, true);
        final TopicToChannelMapper channelMapper = new TopicToChannelMapper(socketMapper);
        final StreamingReader outboundReader =
                new StreamingReader(publisherPageCache, new OutputChannel(channelMapper), true, true);
        return new Service(inboundReader, outboundReader);
    }

    private static final class SocketMapper implements IntFunction<SocketChannel>
    {
        private final Int2ObjectHashMap<SocketAddress> topicToAddress =
                new Int2ObjectHashMap<>();

        @Override
        public SocketChannel apply(final int topicId)
        {
            try
            {
                return SocketChannel.open(topicToAddress.get(topicId));
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        void addAddress(final int topicId, final SocketAddress address)
        {
            topicToAddress.put(topicId, address);
        }
    }
}