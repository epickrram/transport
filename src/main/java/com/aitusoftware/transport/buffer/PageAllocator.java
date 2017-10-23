package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.files.Buffers;
import com.aitusoftware.transport.files.Filenames;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

final class PageAllocator
{
    private static final long MAX_RACE_TIME_SECONDS = 5L;

    private final boolean loadPageIntoMemory = false;
    private final Path path;
    private final int pageSize;
    private final PageIndex pageIndex;

    PageAllocator(final Path path, final int pageSize, final PageIndex pageIndex)
    {
        this.path = path;
        this.pageSize = pageSize;
        this.pageIndex = pageIndex;
    }

    Page safelyAllocatePage(final int pageNumber)
    {
        final Path pagePath = Filenames.forPageNumber(pageNumber, path);
        final long startNanos = System.nanoTime();
        while (!Files.exists(pagePath))
        {
            try
            {
                try (final FileChannel channel = FileChannel.open(pagePath,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                     final RandomAccessFile file = new RandomAccessFile(pagePath.toFile(), "rw"))
                {
                    file.setLength(pageSize + PageHeader.HEADER_SIZE);
                    pageIndex.onPageCreated(pageNumber);
                }
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

        return loadExisting(pageNumber);
    }

    Page loadExisting(final int pageNumber)
    {
        final Path pagePath = Filenames.forPageNumber(pageNumber, path);
        try
        {
            final ByteBuffer buffer = Buffers.map(pagePath, loadPageIntoMemory, pageSize + PageHeader.HEADER_SIZE);
            return new Page(SlabFactory.SLAB_FACTORY.createSlab(buffer), pageNumber, pagePath);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
