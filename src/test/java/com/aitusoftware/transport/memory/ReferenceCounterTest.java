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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ReferenceCounterTest
{
    private final ReferenceCounter counter = new ReferenceCounter();

    @Test
    public void shouldMaintainReferenceCount() throws Exception
    {
        assertTrue(counter.claim());
        assertTrue(counter.claim());
        assertTrue(counter.claim());

        assertThat(counter.getReferenceCount(), is(3));

        counter.release();
        assertThat(counter.getReferenceCount(), is(2));

        counter.release();
        assertThat(counter.getReferenceCount(), is(1));

        counter.release();
        assertThat(counter.getReferenceCount(), is(0));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBlowUpOnDoubleFree() throws Exception
    {
        counter.release();
    }

    @Test
    public void shouldNotBeAbleToMakeUnreachableWhenReferencesExist() throws Exception
    {
        counter.claim();

        assertFalse(counter.makeUnreachable());
    }

    @Test
    public void shouldBeAbleToMakeUnreachable() throws Exception
    {
        assertTrue(counter.makeUnreachable());

        assertFalse(counter.claim());
        assertFalse(counter.claim());
        assertFalse(counter.claim());
    }

    @Test(expected = IllegalStateException.class)
    public void releaseWhenUnreachable() throws Exception
    {
        counter.makeUnreachable();
        counter.release();
    }
}