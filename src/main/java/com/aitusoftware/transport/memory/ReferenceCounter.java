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
package com.aitusoftware.transport.memory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReferenceCounter
{
    private static final int UNREACHABLE = Integer.MIN_VALUE;
    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private long lastClaimedNanos = 0L;

    public boolean claim()
    {
        while (true)
        {
            final int current = referenceCount.get();
            if (current == UNREACHABLE)
            {
                return false;
            }

            if (referenceCount.compareAndSet(current, current + 1))
            {
                lastClaimedNanos = System.nanoTime();
                return true;
            }
        }
    }

    public void release()
    {
        final int countBeforeRelease = referenceCount.getAndDecrement();
        if (countBeforeRelease < 1)
        {
            if (countBeforeRelease == UNREACHABLE)
            {
                throw new IllegalStateException("Reference already unreachable");
            }
            throw new IllegalStateException("Reference Count < 0");
        }
    }

    public int getReferenceCount()
    {
        return referenceCount.get();
    }

    public boolean makeUnreachable()
    {
        return referenceCount.get() == 0 &&
                referenceCount.compareAndSet(0, UNREACHABLE);

    }

    public boolean lastClaimIsOlderThan(final int duration, final TimeUnit unit)
    {
        return lastClaimedNanos + unit.toNanos(duration) < System.nanoTime();
    }

    @Override
    public String toString()
    {
        return "ReferenceCounter{" +
                "referenceCount=" + referenceCount +
                ", lastClaimedNanos=" + lastClaimedNanos +
                '}';
    }
}