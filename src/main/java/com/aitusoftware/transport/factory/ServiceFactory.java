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

import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.messaging.TopicDispatcherRecordHandler;
import com.aitusoftware.transport.messaging.TopicIdCalculator;
import com.aitusoftware.transport.messaging.proxy.AbstractPublisher;
import com.aitusoftware.transport.messaging.proxy.PublisherFactory;
import com.aitusoftware.transport.messaging.proxy.Subscriber;
import com.aitusoftware.transport.messaging.proxy.SubscriberFactory;
import com.aitusoftware.transport.net.AddressSpace;
import com.aitusoftware.transport.net.MultiChannelTopicMessageHandler;
import com.aitusoftware.transport.net.OutputChannel;
import com.aitusoftware.transport.net.Server;
import com.aitusoftware.transport.net.ServerSocketFactory;
import com.aitusoftware.transport.net.SingleChannelTopicMessageHandler;
import com.aitusoftware.transport.net.TopicMessageHandler;
import com.aitusoftware.transport.net.TopicToChannelMapper;
import com.aitusoftware.transport.reader.CopyingRecordHandler;
import com.aitusoftware.transport.reader.StreamingReader;
import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.IntHashSet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import static com.aitusoftware.transport.factory.Named.named;
import static com.aitusoftware.transport.net.FilteringTopicMessageHandler.filter;

public final class ServiceFactory
{
    public static final String PUBLISHER_PAGE_CACHE_PATH = "pub";
    public static final String SUBSCRIBER_PAGE_CACHE_PATH = "sub";
    public static final int PAGE_SIZE = 4096 * 64;

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
    private final Function<Class<?>, Idler> publisherIdlerFactory;
    private final ToIntFunction<Class<?>> topicToSubscriberIndexMapper;
    private final SubscriberThreading subscriberThreading;
    private final Int2ObjectHashMap<Media[]> publisherMedia = new Int2ObjectHashMap<>();
    private final Collection<Named<StreamingReader>> localIpcReaders = new ArrayList<>();
    private final IdlerConfig idlerConfig;
    private boolean hasRemoteSubscribers = false;

    public ServiceFactory(
            final Path pageCachePath, final ServerSocketFactory socketFactory,
            final AddressSpace addressSpace,
            final ToIntFunction<Class<?>> topicToSubscriberIndexMapper,
            final Function<Class<?>, Idler> publisherIdlerFactory,
            final SubscriberThreading subscriberThreading,
            final IdlerConfig idlerConfig) throws IOException
    {
        createRequiredDirectories(pageCachePath);
        this.idlerConfig = idlerConfig;
        publisherPageCache = PageCache.create(publisherDirectory(pageCachePath), PAGE_SIZE);
        subscriberPageCache = PageCache.create(subscriberDirectory(pageCachePath), PAGE_SIZE);
        this.addressSpace = addressSpace;
        this.topicToSubscriberIndexMapper = topicToSubscriberIndexMapper;
        publisherFactory = new PublisherFactory(publisherPageCache);
        subscriberFactory = new SubscriberFactory();
        this.socketFactory = socketFactory;
        this.publisherIdlerFactory = publisherIdlerFactory;
        this.subscriberThreading = subscriberThreading;
    }

    public ServiceFactory(
            final Path pageCachePath, final ServerSocketFactory socketFactory,
            final AddressSpace addressSpace, final Function<Class<?>, Idler> publisherIdlerFactory,
            final SubscriberThreading subscriberThreading,
            final IdlerConfig idlerConfig) throws IOException
    {
        this(pageCachePath, socketFactory, addressSpace, cls -> 0, publisherIdlerFactory, subscriberThreading, idlerConfig);
    }

    public <T> T createPublisher(final Class<T> topicDefinition, final Media... media)
    {
        final T publisher = publisherFactory.getPublisherProxy(topicDefinition);
        publishers.add((AbstractPublisher) publisher);
        topicIdToTopic.put(((AbstractPublisher) publisher).getTopicId(), topicDefinition);
        publisherMedia.put(((AbstractPublisher) publisher).getTopicId(), media);
        Arrays.sort(media);
        return publisher;
    }

    public <T> void registerRemoteSubscriber(final SubscriberDefinition<T> definition)
    {
        final int topicId = TopicIdCalculator.calculate(definition.getTopic());

        if (topicToSubscriber.containsKey(topicId))
        {
            throw new IllegalArgumentException(String.format(
                    "Cannot have more than one subscriber for %s", definition.getTopic()));
        }

        final Subscriber<T> subscriber = subscriberFactory.getSubscriber(definition.getTopic(),
                definition.getImplementation());
        subscribers.add(subscriber);
        topicToSubscriber.put(topicId, subscriber);
        final List<SocketAddress> socketAddresses = addressSpace.addressesOf(definition.getTopic());
        socketFactory.registerTopicAddress(topicId, socketAddresses.get(
                topicToSubscriberIndexMapper.applyAsInt(definition.getTopic())));
        topicIds.add(topicId);
        topicIdToTopic.put(topicId, definition.getTopic());
        hasRemoteSubscribers = true;
    }

    public <T> void registerLocalSubscriber(
            final SubscriberDefinition<T> definition, final Path localPublisherPageCachePath)
    {
        final int topicId = TopicIdCalculator.calculate(definition.getTopic());

        if (topicToSubscriber.containsKey(topicId))
        {
            throw new IllegalArgumentException(String.format(
                    "Cannot have more than one subscriber for %s", definition.getTopic()));
        }

        final Subscriber<T> subscriber = subscriberFactory.getSubscriber(definition.getTopic(),
                definition.getImplementation());
        subscribers.add(subscriber);
        topicToSubscriber.put(topicId, subscriber);

        try
        {
            final StreamingReader outboundReader =
                    new StreamingReader(PageCache.create(localPublisherPageCachePath, PAGE_SIZE),
                    new CopyingRecordHandler(subscriberPageCache),
                    true,
                    // TODO configure through SubscriberIdlerFactory
                    AdaptiveIdlerFactory.idleUpTo(1, TimeUnit.MILLISECONDS).apply(definition.getTopic()));
            localIpcReaders.add(named("local-subscriber-" +
                    definition.getTopic().getSimpleName(), outboundReader));
            readers.add(outboundReader);

            topicIds.add(topicId);
            topicIdToTopic.put(topicId, definition.getTopic());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public Service create()
    {
        final TopicDispatcherRecordHandler topicDispatcher =
                new TopicDispatcherRecordHandler(topicToSubscriber);

        final StreamingReader inboundReader =
                new StreamingReader(subscriberPageCache, topicDispatcher, true,
                        Idlers.staticPause(1, TimeUnit.MILLISECONDS));
        final TopicToChannelMapper channelMapper = new TopicToChannelMapper(socketMapper);

        final Collection<Named<StreamingReader>> namedPublishers = createPublisherReaders(channelMapper);
        readers.add(inboundReader);
        final Server server = new Server(topicIds, socketFactory::acquire, subscriberPageCache,
                subscriberThreading, topicIdToTopic);
        final Collection<Named<StreamingReader>> namedReaders = new ArrayList<>(namedPublishers);
        namedReaders.addAll(localIpcReaders);
        return new Service(inboundReader, namedReaders, server, hasRemoteSubscribers);
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

    private Collection<Named<StreamingReader>> createPublisherReaders(final TopicToChannelMapper channelMapper)
    {
        final Collection<Named<StreamingReader>> namedPublishers = new ArrayList<>(publishers.size());
        publishers.forEach(publisher -> {
            final int topicId = publisher.getTopicId();
            if (Arrays.binarySearch(publisherMedia.get(topicId), Media.TCP) < 0)
            {
                return;
            }

            final Class<?> topicDefinition = topicIdToTopic.get(topicId);

            final List<SocketAddress> receiverAddresses = addressSpace.addressesOf(topicDefinition);
            final TopicMessageHandler messageHandler;
            if (receiverAddresses.size() == 1)
            {
                socketMapper.addAddress(TopicIdCalculator.calculate(topicDefinition),
                        addressSpace.addressOf(topicDefinition));
                messageHandler = new SingleChannelTopicMessageHandler(channelMapper);
            }
            else
            {
                messageHandler = new MultiChannelTopicMessageHandler(
                        new TopicToChannelMapper(i -> connectSocket(receiverAddresses.get(i))),
                        receiverAddresses.size());
            }
            final OutputChannel outputChannel = new OutputChannel(
                    filter(topicId, messageHandler));
            final StreamingReader outboundReader =
                    new StreamingReader(publisherPageCache, outputChannel,
                            true, publisherIdlerFactory.apply(topicDefinition));
            namedPublishers.add(named("outbound-publisher-" +
                    topicIdToTopic.get(publisher.getTopicId()).getSimpleName(), outboundReader));
            readers.add(outboundReader);
        });
        return namedPublishers;
    }
    private static final class SocketMapper implements IntFunction<SocketChannel>
    {

        private final Int2ObjectHashMap<SocketAddress> topicToAddress =
                new Int2ObjectHashMap<>();

        @Override
        public SocketChannel apply(final int topicId)
        {
            final SocketAddress socketAddress = topicToAddress.get(topicId);
            return connectSocket(socketAddress);
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

    private static SocketChannel connectSocket(final SocketAddress socketAddress)
    {
        try
        {
            final SocketChannel channel = SocketChannel.open(socketAddress);
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

    private static Path subscriberDirectory(final Path pageCachePath)
    {
        return pageCachePath.resolve(SUBSCRIBER_PAGE_CACHE_PATH);
    }

    private static Path publisherDirectory(final Path pageCachePath)
    {
        return pageCachePath.resolve(PUBLISHER_PAGE_CACHE_PATH);
    }

    private static void createRequiredDirectories(final Path pageCachePath) throws IOException
    {
        Files.createDirectories(publisherDirectory(pageCachePath));
        Files.createDirectories(subscriberDirectory(pageCachePath));
    }
}