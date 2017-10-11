package com.aitusoftware.transport.files;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public enum Buffers
{
    BUFFERS;

    public static ByteBuffer map(final Path path, final boolean forceLoad, final long size) throws IOException
    {
        try
        {
            final FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            new RandomAccessFile(path.toFile(), "rw").setLength(size);
        }
        catch (IOException e)
        {
            // already created
        }

        final FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.READ);
        final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
        if (forceLoad)
        {
            buffer.load();
        }
        channel.close();

        return buffer;
    }
}