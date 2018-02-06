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
import java.util.concurrent.locks.LockSupport;

@SingleThreaded
final class AdaptiveIdler implements Idler
{
    private static final int INITIAL_COUNT = 200;
    private final long maxPause;
    private final TimeUnit pauseUnit;
    private int spinCount = INITIAL_COUNT;
    private int yieldCount = INITIAL_COUNT;
    private long nextPause;

    AdaptiveIdler(final long maxPause, final TimeUnit pauseUnit)
    {
        this.maxPause = maxPause;
        this.pauseUnit = pauseUnit;
    }

    @Override
    public void idle()
    {
        if (spinCount != 0)
        {
            spinCount--;
            return;
        }

        if (yieldCount != 0)
        {
            yieldCount--;
            return;
        }

        LockSupport.parkNanos(nextPause);
        nextPause = Math.min(nextPause * 2, pauseUnit.toNanos(maxPause));
    }

    @Override
    public void reset()
    {
        spinCount = INITIAL_COUNT;
        yieldCount = INITIAL_COUNT;
        nextPause = 1L;
    }
}
