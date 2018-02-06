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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OffsetsTest
{
    private final Offsets offsets = new Offsets(64);

    @Test
    public void shouldCalculateOffset() throws Exception
    {
        assertThat(offsets.pageOffset(0L), is(0));
        assertThat(offsets.pageOffset(1L), is(1));
        assertThat(offsets.pageOffset(63L), is(63));
        assertThat(offsets.pageOffset(64L), is(0));
        assertThat(offsets.pageOffset(65L), is(1));
        assertThat(offsets.pageOffset(127L), is(63));
        assertThat(offsets.pageOffset(128L), is(0));
        assertThat(offsets.pageOffset(129L), is(1));
    }

    @Test
    public void shouldCalculatePageNumber() throws Exception
    {
        assertThat(offsets.pageNumber(0L), is(0));
        assertThat(offsets.pageNumber(63L), is(0));
        assertThat(offsets.pageNumber(64L), is(1));
        assertThat(offsets.pageNumber(65L), is(1));
        assertThat(offsets.pageNumber(127L), is(1));
        assertThat(offsets.pageNumber(128L), is(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequirePowerOfTwo() throws Exception
    {
        new Offsets(65);
    }
}