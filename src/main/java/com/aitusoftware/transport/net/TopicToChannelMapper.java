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
package com.aitusoftware.transport.net;

import com.aitusoftware.transport.threads.SingleThreaded;
import org.agrona.collections.Int2ObjectHashMap;

import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SocketChannel;
import java.util.function.IntFunction;

@SingleThreaded
public final class TopicToChannelMapper
{
    private final IntFunction<SocketChannel> connector;
    private final Int2ObjectHashMap<SocketChannel> openChannels =
            new Int2ObjectHashMap<>();

    public TopicToChannelMapper(final IntFunction<SocketChannel> connector)
    {
        this.connector = connector;
    }

    GatheringByteChannel forTopic(final int topicId)
    {
        SocketChannel channel = openChannels.get(topicId);
        if (channel != null)
        {
            return channel;
        }
        final SocketChannel connected = connector.apply(topicId);
        openChannels.put(topicId, connected);
        return connected;
    }

    void reconnectChannel(final int topicId)
    {
        openChannels.remove(topicId);
    }
}
