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

import java.nio.ByteBuffer;

public final class FilteringTopicMessageHandler implements TopicMessageHandler
{
    private final int topicId;
    private final TopicMessageHandler delegate;

    private FilteringTopicMessageHandler(final int topicId, final TopicMessageHandler delegate)
    {
        this.topicId = topicId;
        this.delegate = delegate;
    }

    @Override
    public void onTopicMessage(final int topicId, final ByteBuffer data)
    {
        if (topicId == this.topicId)
        {
            delegate.onTopicMessage(topicId, data);
        }
    }

    public static TopicMessageHandler filter(final int topicId, final TopicMessageHandler delegate)
    {
        return new FilteringTopicMessageHandler(topicId, delegate);
    }
}