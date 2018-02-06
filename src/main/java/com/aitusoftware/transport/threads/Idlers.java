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
package com.aitusoftware.transport.threads;

import java.util.concurrent.TimeUnit;

public final class Idlers
{
    private Idlers()
    {
    }

    public static Idler busy()
    {
        return new BusySpinIdler();
    }

    public static Idler yielding()
    {
        return new YieldingIdler();
    }

    public static Idler adaptive(final long maxPause, final TimeUnit pauseUnit)
    {
        return new AdaptiveIdler(maxPause, pauseUnit);
    }

    public static Idler staticPause(final long pause, final TimeUnit pauseUnit)
    {
        return new StaticPausingIdler(pause, pauseUnit);
    }

    public static Idler forString(final String spec)
    {
        final String[] tokens = spec.split(",");
        try
        {
            switch (tokens[0])
            {
                case "BUSY_SPIN":
                    return busy();
                case "YIELDING":
                    return yielding();
                case "STATIC":
                    return staticPauseFromSpec(tokens);
                case "ADAPTIVE":
                    return adaptivePauseFromSpec(tokens);
                default:
                    return reject(spec);
            }
        }
        catch (IllegalArgumentException e)
        {
            return reject(spec);
        }
    }

    private static Idler adaptivePauseFromSpec(final String[] tokens)
    {
        return adaptive(Long.parseLong(tokens[1]), TimeUnit.valueOf(tokens[2]));
    }

    private static Idler staticPauseFromSpec(final String[] tokens)
    {
        return staticPause(Long.parseLong(tokens[1]), TimeUnit.valueOf(tokens[2]));
    }

    private static Idler reject(final String spec)
    {
        throw new IllegalArgumentException(String.format("Unrecognised Idler spec: %s", spec));
    }
}
