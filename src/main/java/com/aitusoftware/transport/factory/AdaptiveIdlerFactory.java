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

import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class AdaptiveIdlerFactory
{
    private final long maxPause;
    private final TimeUnit pauseUnit;

    private AdaptiveIdlerFactory(final long maxPause, final TimeUnit pauseUnit)
    {
        this.maxPause = maxPause;
        this.pauseUnit = pauseUnit;
    }

    @SuppressWarnings("unused")
    private Idler forPublisher(final Class<?> topicClass)
    {
        return Idlers.adaptive(maxPause, pauseUnit);
    }

    public static Function<Class<?>, Idler> idleUpTo(final long maxPause, final TimeUnit pauseUnit)
    {
        return new AdaptiveIdlerFactory(maxPause, pauseUnit)::forPublisher;
    }
}
