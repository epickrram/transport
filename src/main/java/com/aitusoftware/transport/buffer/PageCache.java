package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.files.Directories;
import com.aitusoftware.transport.files.Filenames;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.file.Files;
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

    private static final ThreadLocal<WritableRecord> RECORD_BUFFER =
            ThreadLocal.withInitial(WritableRecord::new);
    private final PageAllocator allocator;
    private final Path path;
    private final int pageSize;
    private volatile Page currentPage;
    private volatile int currentPageNumber;

    private PageCache(final int pageSize, final Path path)
    {
        // TODO should handle initialisation from existing file-system resources
        this.path = path;
        allocator = new PageAllocator(this.path, pageSize);
        CURRENT_PAGE_VH.setRelease(this, allocator.safelyAllocatePage(INITIAL_PAGE_NUMBER));
        CURRENT_PAGE_NUMBER_VH.setRelease(this, INITIAL_PAGE_NUMBER);
        this.pageSize = pageSize;
    }

    public WritableRecord acquireRecordBuffer(final int recordLength)
    {
        final Page page = (Page) CURRENT_PAGE_VH.getVolatile(this);
        final int position = page.acquireSpaceInBuffer(recordLength);

        if (position >= 0)
        {
            final WritableRecord record = RECORD_BUFFER.get();
            record.set(page.slice(position, recordLength), page, position);
            return record;
        }
        else if (position == Page.ERR_MESSAGE_TOO_LARGE)
        {
            throw new RuntimeException(String.format(
                    "Message too large for current page: %s", currentPage));
        }
        else if (position == Page.ERR_NOT_ENOUGH_SPACE)
        {
            int pageNumber = page.getPageNumber();
            while (!Thread.currentThread().isInterrupted())
            {
                if (((int) CURRENT_PAGE_NUMBER_VH.get(this)) > pageNumber &&
                        ((Page) CURRENT_PAGE_VH.get(this)).getPageNumber() == pageNumber)
                {
                    // another write has won, and will allocate a new page
                    while ((((Page) CURRENT_PAGE_VH.get(this)).getPageNumber() == pageNumber))
                    {
                        Thread.yield();
                    }

                    break;
                }
                pageNumber = (int) CURRENT_PAGE_NUMBER_VH.get(this);
                if (CURRENT_PAGE_NUMBER_VH.compareAndSet(this, pageNumber, pageNumber + 1))
                {
                    // this thread won, allocate a new page
                    CURRENT_PAGE_VH.setRelease(this, allocator.safelyAllocatePage(pageNumber + 1));
                    break;
                }
            }
            return acquireRecordBuffer(recordLength);
        }
        else
        {
            throw new IllegalStateException();
        }
    }

    // contain page-cache header
    void append(final ByteBuffer source)
    {
        final WritableRecord record = acquireRecordBuffer(source.remaining());
        record.buffer().put(source);
        record.commit();
    }

    public long estimateTotalLength()
    {
        final Page page = (Page) CURRENT_PAGE_VH.get(this);
        return ((long) page.getPageNumber()) * page.totalDataSize() +
                page.nextAvailablePosition();
    }

    public boolean isPageAvailable(final int pageNumber)
    {
        // optimisation - cache file names
        return Files.exists(Filenames.forPageNumber(pageNumber, path));
    }

    public Page getPage(final int pageNumber)
    {
        // optimisation - cache pages
        return allocator.loadExisting(pageNumber);
    }

    public int getPageSize()
    {
        return pageSize;
    }

    public static PageCache create(final Path path, final int pageSize)
    {
        Directories.ensureDirectoryExists(path);

        return new PageCache(pageSize, path);
    }
}
