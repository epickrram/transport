package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.files.Filenames;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

public final class PageAllocator
{
    private static final long MAX_RACE_TIME_SECONDS = 5L;

    private final boolean loadPageIntoMemory = false;
    private final Path path;

    public PageAllocator(final Path path)
    {
        this.path = path;
    }

    Page safelyAllocatePage(final int pageSize, final int pageNumber)
    {
        final Path pagePath = Filenames.forPageNumber(pageNumber, path);
        final long startNanos = System.nanoTime();
        while (!Files.exists(pagePath))
        {
            try
            {
                FileChannel.open(pagePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                new RandomAccessFile(pagePath.toFile(), "rw").setLength(pageSize + PageHeader.HEADER_SIZE);
            }
            catch (IOException e)
            {
                if (System.nanoTime() > startNanos + TimeUnit.SECONDS.toNanos(MAX_RACE_TIME_SECONDS))
                {
                    throw new IllegalStateException(String.format(
                            "Unable to create file at %s", pagePath), e);
                }
            }
        }

        try
        {
            final FileChannel channel = FileChannel.open(pagePath, StandardOpenOption.WRITE, StandardOpenOption.READ);
            final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, pageSize + PageHeader.HEADER_SIZE);
            if (loadPageIntoMemory)
            {
                buffer.load();
            }
            return new Page(SlabFactory.SLAB_FACTORY.createSlab(buffer), pageNumber);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
