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
package com.aitusoftware.transport.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class Buffers
{
    private Buffers()
    {
    }

    public static ByteBuffer map(final Path path, final long size) throws IOException
    {
        final FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.READ);
        final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
        channel.close();

        return buffer;
    }
}