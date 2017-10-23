package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.files.Directories;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.file.Path;

/**
 * The entry-point to the journal containing records.
 *
 * Safe for reading and writing by multiple threads.
 *
 * Maintains a 'current' {@link Page} for writing.
 */
public final class PageCache
{
    private static final VarHandle CURRENT_PAGE_VH;
    private static final VarHandle CURRENT_PAGE_NUMBER_VH;
    private static final int INITIAL_PAGE_NUMBER = 0;
    private static final int CACHED_PAGE_COUNT = 32;
    private static final int CACHED_PAGE_MASK = CACHED_PAGE_COUNT - 1;

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
    private final PageIndex pageIndex;
    private final CachedPage[] cachedPages = new CachedPage[CACHED_PAGE_COUNT];
    private volatile Page currentPage;
    private volatile int currentPageNumber;

    private PageCache(final int pageSize, final Path path, final PageIndex pageIndex)
    {
        // TODO should handle initialisation from existing file-system resources
        this.path = path;
        allocator = new PageAllocator(this.path, pageSize, pageIndex);
        CURRENT_PAGE_VH.setRelease(this, allocator.safelyAllocatePage(INITIAL_PAGE_NUMBER));
        CURRENT_PAGE_NUMBER_VH.setRelease(this, INITIAL_PAGE_NUMBER);
        this.pageSize = pageSize;
        this.pageIndex = pageIndex;
    }

    /**
     * Acquire a slice in the underlying page with specified capacity.
     *
     * @param recordLength required capacity
     * @return placeholder for data
     */
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
                page.tryWriteEof();
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

    /**
     * Estimates the total data size contained in the page cache directory.
     *
     * @return total length in bytes
     */
    public long estimateTotalLength()
    {
        final Page page = (Page) CURRENT_PAGE_VH.get(this);
        return ((long) page.getPageNumber()) * page.totalDataSize() +
                page.nextAvailablePosition();
    }

    /**
     * Tests whether a page is available in the page-cache directory
     *
     * @param pageNumber to test for
     * @return whether the page is available for reading
     */
    public boolean isPageAvailable(final int pageNumber)
    {
        if (pageIndex.isLessThanLowestTrackedPageNumber(pageNumber))
        {
            // TODO fall-back to file-system lookup
        }
        return pageIndex.isPageCreated(pageNumber);
    }

    /**
     * Load a page from the page-cache
     *
     * @param pageNumber the number of an existing page
     * @return the {@link Page}
     */
    public Page getPage(final int pageNumber)
    {
        final int cachedPageIndex = toCachedPageIndex(pageNumber);
        CachedPage cachedPage = cachedPages[cachedPageIndex];
        if (cachedPage != null)
        {
            final Page page = cachedPage.page;
            if (page.getPageNumber() == pageNumber)
            {
                cachedPage.lastAccessedNanos = System.nanoTime();
                cachedPages[cachedPageIndex] = cachedPage;
                return page;
            }
        }
        else
        {
            cachedPage = new CachedPage();
        }

        final Page existing = allocator.loadExisting(pageNumber);
        cachedPage.page = existing;
        cachedPage.lastAccessedNanos = System.nanoTime();
        cachedPages[cachedPageIndex] = cachedPage;
        return existing;
    }

    Page allocate(final int pageNumber)
    {
        allocator.safelyAllocatePage(pageNumber);
        return getPage(pageNumber);
    }

    // not thread-safe consider removing
    public void read(final int pageNumber, final int position, final ByteBuffer buffer)
    {
        getPage(pageNumber).read(position, buffer);
    }

    public ByteBuffer slice(final int pageNumber, final int position, final int recordLength)
    {
        return getPage(pageNumber).slice(position, recordLength);
    }

    /**
     * Retrieve the size of each page in the cache
     *
     * @return page size in bytes
     */
    public int getPageSize()
    {
        return pageSize;
    }

    public PageIndex getPageIndex()
    {
        return pageIndex;
    }

    /**
     * Create a page-cache in the specified directory
     *
     * @param path file-system path in which to store data
     * @param pageSize size of each page in bytes
     * @return the PageCache
     * @throws IOException if the page-cache cannot be initialised
     */
    public static PageCache create(final Path path, final int pageSize) throws IOException
    {
        Directories.ensureDirectoryExists(path);
        final PageIndex pageIndex = PageIndex.forPageCache(path);

        return new PageCache(pageSize, path, pageIndex);
    }

    private static int toCachedPageIndex(final int pageNumber)
    {
        return pageNumber & CACHED_PAGE_MASK;
    }

    private static final class CachedPage
    {
        private Page page;
        private volatile long lastAccessedNanos;
    }
}