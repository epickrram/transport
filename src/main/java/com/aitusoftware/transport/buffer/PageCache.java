package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.files.Directories;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public final class PageCache {
    private static final VarHandle CURRENT_PAGE_VH;

    static
    {
        try
        {
            CURRENT_PAGE_VH = MethodHandles.lookup().
                    findVarHandle(PageCache.class, "currentPage", Page.class);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new IllegalStateException("Unable to obtain VarHandle");
        }

    }

    private final int pageSize;
    private volatile Page currentPage;

    public PageCache(final int pageSize)
    {
        this.pageSize = pageSize;
        CURRENT_PAGE_VH.setRelease(this, new Page(SlabFactory.SLAB_FACTORY.createSlab(pageSize)));
    }

    // contain page-cache header
    void append(final ByteBuffer source) {
        final Page page = (Page) CURRENT_PAGE_VH.getVolatile(this);
        try
        {
            final WriteResult writeResult = page.write(source);
            switch (writeResult)
            {
                case SUCCESS:
                    return;
                case FAILURE:
                    throw new RuntimeException(String.format(
                            "Failed to append to current page: %s", currentPage));
                case MESSAGE_TOO_LARGE:
                    throw new RuntimeException(String.format(
                            "Message too large for current page: %s", currentPage));
                case NOT_ENOUGH_SPACE:
                    handleOverflow(source);
            }
        }
        catch (RuntimeException e)
        {
            throw new RuntimeException(String.format(
                    "Failed to write to current page: %s", currentPage
            ), e);
        }
    }

    long estimateTotalLength()
    {
        return 0L;
    }

    private void handleOverflow(final ByteBuffer message)
    {
        throw new UnsupportedOperationException();
    }

    static PageCache create(final Path path, final int pageSize)
    {
        Directories.ensureDirectoryExists(path);

        return new PageCache(pageSize);
    }
}
