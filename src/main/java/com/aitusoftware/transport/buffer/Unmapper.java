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
package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public final class Unmapper
{
    private final Queue<Page> activePages = new ConcurrentLinkedQueue<>();
    private final Idler idler = Idlers.staticPause(1, TimeUnit.MILLISECONDS);

    void registerPage(final Page page)
    {
        activePages.add(page);
    }

    public void execute()
    {
        while (!Thread.currentThread().isInterrupted())
        {
            boolean idle = true;
            final Set<Page> forRemoval = new HashSet<>();
            for (Page page : activePages)
            {
                if (page.acquireForCleanup())
                {
                    idle = false;
                    page.unmap();
                    forRemoval.add(page);
                }
            }
            activePages.removeAll(forRemoval);

            if (idle)
            {
                idler.idle();
            }
        }
    }
}
