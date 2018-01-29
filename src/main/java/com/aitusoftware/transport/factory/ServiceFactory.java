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
import com.aitusoftware.transport.net.SingleChannelTopicMessageHandler;
import com.aitusoftware.transport.net.TopicToChannelMapper;
import com.aitusoftware.transport.reader.StreamingReader;
import com.aitusoftware.transport.threads.Idlers;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.IntHashSet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static com.aitusoftware.transport.factory.Named.named;
import static com.aitusoftware.transport.net.FilteringTopicMessageHandler.filter;

public final class ServiceFactory
{
    public static final String PUBLISHER_PAGE_CACHE_PATH = "pub";
    public static final String SUBSCRIBER_PAGE_CACHE_PATH = "sub";
    public static final int PAGE_SIZE = 1 << 18;

    private final PublisherFactory publisherFactory;
    private final PageCache subscriberPageCache;
    private final AddressSpace addressSpace;
    private final Int2ObjectHashMap<Subscriber> topicToSubscriber = new Int2ObjectHashMap<>();
    private final Int2ObjectHashMap<Class<?>> topicIdToTopic = new Int2ObjectHashMap<>();
    private final IntHashSet topicIds = new IntHashSet();
    private final SubscriberFactory subscriberFactory;
    private final PageCache publisherPageCache;
    private final SocketMapper socketMapper = new SocketMapper();
    private final List<AbstractPublisher> publishers = new ArrayList<>();
    private final List<Subscriber<?>> subscribers = new ArrayList<>();
    private final List<StreamingReader> readers = new ArrayList<>();
    private final ServerSocketFactory socketFactory;
    private final PublisherIdlerFactory publisherIdlerFactory = new PublisherIdlerFactory();

    public ServiceFactory(
            final Path pageCachePath, final ServerSocketFactory socketFactory,
            final AddressSpace addressSpace) throws IOException
    {
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
        topicIdToTopic.put(((AbstractPublisher) publisher).getTopicId(), topicDefinition);

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
                new StreamingReader(subscriberPageCache, topicDispatcher, true, Idlers.staticPause(1, TimeUnit.MILLISECONDS));
        final TopicToChannelMapper channelMapper = new TopicToChannelMapper(socketMapper);

        final Collection<Named<StreamingReader>> namedPublishers = new ArrayList<>(publishers.size());
        publishers.forEach(publisher -> {
            final int topicId = publisher.getTopicId();
            final OutputChannel outputChannel = new OutputChannel(
                    filter(topicId, new SingleChannelTopicMessageHandler(channelMapper)));
            final StreamingReader outboundReader =
                    new StreamingReader(publisherPageCache, outputChannel,
                            true, publisherIdlerFactory.forPublisher(topicIdToTopic.get(topicId)));
            namedPublishers.add(named("publisher-" +
                    topicIdToTopic.get(publisher.getTopicId()).getSimpleName(), outboundReader));
            readers.add(outboundReader);
        });
        readers.add(inboundReader);
        final Server server = new Server(topicIds, socketFactory::acquire, subscriberPageCache);
        return new Service(inboundReader, namedPublishers, server);
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
                while (!channel.finishConnect())
                {
                    Thread.yield();
                }

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