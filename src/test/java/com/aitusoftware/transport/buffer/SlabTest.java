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

import com.aitusoftware.transport.memory.BufferUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SlabTest
{
    private final long mappedBufferCount = BufferUtil.mappedBufferCount();
    private Slab slab;

    @Before
    public void setUp() throws Exception
    {
        final FileChannel channel = FileChannel.open(File.createTempFile("first", "last").toPath(),
                StandardOpenOption.WRITE, StandardOpenOption.READ);
        final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 64);
        slab = new Slab(buffer);
    }

    @Test
    public void shouldCasAtOffset() throws Exception
    {
        assertThat(slab.getLongVolatile(8), is(0L));
        assertThat(slab.compareAndSetLong(8, 0L, 17L), is(true));
        assertThat(slab.getLongVolatile(8), is(17L));
    }

    @Test
    public void shouldGetAndAddAtOffset() throws Exception
    {
        assertThat(slab.getLongVolatile(8), is(0L));
        assertThat(slab.getAndAddLong(8, 17L), is(0L));
        assertThat(slab.getAndAddLong(8, 11L), is(17L));
        assertThat(slab.getLongVolatile(8), is(28L));
    }

    @Test
    public void unmapTest() throws Exception
    {
        assertThat(BufferUtil.mappedBufferCount(), is(mappedBufferCount + 1));

        slab.unmap();

        assertThat(BufferUtil.mappedBufferCount(), is(mappedBufferCount));
    }
}