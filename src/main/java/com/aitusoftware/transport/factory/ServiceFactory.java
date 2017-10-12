package com.aitusoftware.transport.factory;

import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.messaging.TopicDispatcherRecordHandler;
import com.aitusoftware.transport.messaging.TopicIdCalculator;
import com.aitusoftware.transport.messaging.proxy.AbstractPublisher;
import com.aitusoftware.transport.messaging.proxy.PublisherFactory;
import com.aitusoftware.transport.messaging.proxy.Subscriber;
import com.aitusoftware.transport.messaging.proxy.SubscriberFactory;
import com.aitusoftware.transport.net.AddressSpace;
import com.aitusoftware.transport.net.OutputChannel;
import com.aitusoftware.transport.net.Server;
import com.aitusoftware.transport.net.ServerSocketFactory;
import com.aitusoftware.transport.net.TopicToChannelMapper;
import com.aitusoftware.transport.reader.StreamingReader;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.IntHashSet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public final class ServiceFactory
{
    public static final String PUBLISHER_PAGE_CACHE_PATH = "pub";
    public static final String SUBSCRIBER_PAGE_CACHE_PATH = "sub";
    public static final int PAGE_SIZE = 1 << 18;

    private final Path pageCachePath;
    private final PublisherFactory publisherFactory;
    private final PageCache subscriberPageCache;
    private final AddressSpace addressSpace;
    private final Int2ObjectHashMap<Subscriber> topicToSubscriber = new Int2ObjectHashMap<>();
    private final IntHashSet topicIds = new IntHashSet();
    private final SubscriberFactory subscriberFactory;
    private final PageCache publisherPageCache;
    private final SocketMapper socketMapper = new SocketMapper();
    private final List<AbstractPublisher> publishers = new ArrayList<>();
    private final List<Subscriber<?>> subscribers = new ArrayList<>();
    private final List<StreamingReader> readers = new ArrayList<>();
    private final ServerSocketFactory socketFactory;

    public ServiceFactory(
            final Path pageCachePath, final ServerSocketFactory socketFactory,
            final AddressSpace addressSpace) throws IOException
    {
        this.pageCachePath = pageCachePath;
        publisherPageCache = PageCache.create(pageCachePath.resolve(PUBLISHER_PAGE_CACHE_PATH), PAGE_SIZE);
        subscriberPageCache = PageCache.create(pageCachePath.resolve(SUBSCRIBER_PAGE_CACHE_PATH), PAGE_SIZE);
        this.addressSpace = addressSpace;
        publisherFactory = new PublisherFactory(publisherPageCache);
        subscriberFactory = new SubscriberFactory();
        this.socketFactory = socketFactory;
    }

    public <T> T createPublisher(final Class<T> topicDefinition)
    {
        final T publisher = publisherFactory.getPublisherProxy(topicDefinition);
        publishers.add((AbstractPublisher) publisher);
        socketMapper.addAddress(TopicIdCalculator.calculate(topicDefinition),
                addressSpace.addressOf(topicDefinition));
        return publisher;
    }

    public <T> void registerSubscriber(final SubscriberDefinition<T> definition)
    {
        final int topicId = TopicIdCalculator.calculate(definition.getTopic());
        final Subscriber<T> subscriber = subscriberFactory.getSubscriber(definition.getTopic(), definition.getImplementation());
        subscribers.add(subscriber);
        topicToSubscriber.put(topicId, subscriber);
        socketFactory.registerTopicAddress(topicId, addressSpace.addressOf(definition.getTopic()));
        topicIds.add(topicId);
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
        readers.add(inboundReader);
        readers.add(outboundReader);
        final Server server = new Server(topicIds, socketFactory::acquire, subscriberPageCache);
        return new Service(inboundReader, outboundReader, server);
    }

    public void publishers(final Consumer<AbstractPublisher> consumer)
    {
        publishers.forEach(consumer);
    }

    public void subscribers(final Consumer<Subscriber<?>> consumer)
    {
        subscribers.forEach(consumer);
    }

    public void readers(final Consumer<StreamingReader> consumer)
    {
        readers.forEach(consumer);
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
                final SocketChannel channel = SocketChannel.open(topicToAddress.get(topicId));
                channel.configureBlocking(false);
                return channel;
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        void addAddress(final int topicId, final SocketAddress address)
        {
            if (topicToAddress.containsKey(topicId))
            {
                throw new IllegalStateException("Already contains an address for " + topicId);
            }
            topicToAddress.put(topicId, address);
        }
    }
}