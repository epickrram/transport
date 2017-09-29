package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.files.Directories;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public final class PageCache
{
    private static final VarHandle CURRENT_PAGE_VH;
    private static final VarHandle CURRENT_PAGE_NUMBER_VH;
    private static final int INITIAL_PAGE_NUMBER = 0;

    static
    {
        try
        {
            CURRENT_PAGE_VH = MethodHandles.lookup().
                    findVarHandle(PageCache.class, "currentPage", Page.class);
            CURRENT_PAGE_NUMBER_VH = MethodHandles.lookup().
                    findVarHandle(PageCache.class, "currentPageNumber", int.class);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new IllegalStateException("Unable to obtain VarHandle");
        }

    }

    private final int pageSize;
    private volatile Page currentPage;
    private volatile int currentPageNumber;

    PageCache(final int pageSize)
    {
        // TODO should handle initialisation from existing file-system resources
        this.pageSize = pageSize;
        CURRENT_PAGE_VH.setRelease(this, new Page(SlabFactory.SLAB_FACTORY.createSlab(pageSize + PageHeader.HEADER_SIZE), INITIAL_PAGE_NUMBER));
        CURRENT_PAGE_NUMBER_VH.setRelease(this, INITIAL_PAGE_NUMBER);
    }

    // contain page-cache header
    void append(final ByteBuffer source)
    {
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
                    handleOverflow(source, page);
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
        final Page page = (Page) CURRENT_PAGE_VH.get(this);
        return page.getPageNumber() * page.totalDataSize() +
                page.nextAvailablePosition();
    }

    private void handleOverflow(final ByteBuffer message, final Page page)
    {
        final int pageNumber = page.getPageNumber();
        while (!Thread.currentThread().isInterrupted())
        {
            if (((int) CURRENT_PAGE_NUMBER_VH.get(this)) == pageNumber + 1 &&
                    ((Page) CURRENT_PAGE_VH.get(this)).getPageNumber() != pageNumber + 1)
            {
                // another write has won, and will allocate a new page
                while ((((Page) CURRENT_PAGE_VH.get(this)).getPageNumber() != pageNumber + 1))
                {
                    Thread.yield();
                }
                break;
            }
            if (CURRENT_PAGE_NUMBER_VH.compareAndSet(this, pageNumber, pageNumber + 1))
            {
                // this thread won, allocate a new page
                CURRENT_PAGE_VH.setRelease(this, new Page(SlabFactory.SLAB_FACTORY.createSlab(pageSize + PageHeader.HEADER_SIZE), pageNumber + 1));
                break;
            }
        }

        append(message);
    }

    static PageCache create(final Path path, final int pageSize)
    {
        Directories.ensureDirectoryExists(path);

        return new PageCache(pageSize);
    }
}
