package com.aitusoftware.transport.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public enum Buffers
{
    BUFFERS;

    public static ByteBuffer map(final Path path, final long size) throws IOException
    {
        final FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.READ);
        final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
        channel.close();

        return buffer;
    }
}