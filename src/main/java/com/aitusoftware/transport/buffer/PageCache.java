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

import com.aitusoftware.transport.files.Directories;
import com.aitusoftware.transport.files.Filenames;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The entry-point to the journal containing records.
 * <p>
 * Safe for reading and writing by multiple threads.
 * <p>
 * Maintains a 'current' {@link Page} for writing.
 */
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
    private static final ThreadLocal<Slice> SLICE =
            ThreadLocal.withInitial(Slice::new);
    private final PageAllocator allocator;
    private final int pageSize;
    private final PageIndex pageIndex;
    private final Unmapper unmapper = new Unmapper();
    private final LoadedPageCache loadedPageCache;
    private final Path path;
    @SuppressWarnings("unused")
    private volatile Page currentPage;
    @SuppressWarnings("unused")
    private volatile int currentPageNumber;

    private PageCache(final int pageSize, final Path path, final PageIndex pageIndex)
    {
        // TODO should handle initialisation from existing file-system resources
        allocator = new PageAllocator(path, pageSize, pageIndex, unmapper);
        CURRENT_PAGE_VH.setRelease(this, allocator.safelyAllocatePage(INITIAL_PAGE_NUMBER));
        CURRENT_PAGE_NUMBER_VH.setRelease(this, INITIAL_PAGE_NUMBER);
        this.pageSize = pageSize;
        this.pageIndex = pageIndex;
        loadedPageCache = new LoadedPageCache(allocator);
        this.path = path;
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
        if (!page.claimReference())
        {
            return acquireRecordBuffer(recordLength);
        }
        final int position = page.acquireSpaceInBuffer(recordLength);
        if (position >= 0)
        {
            final WritableRecord record = RECORD_BUFFER.get();
            record.set(page.slice(position, recordLength), page, position);
            return record;
        }
        else if (position == Page.ERR_MESSAGE_TOO_LARGE)
        {
            page.releaseReference();
            throw new IllegalArgumentException(String.format(
                    "Message too large for current page: %s", currentPage));
        }
        else if (position == Page.ERR_NOT_ENOUGH_SPACE)
        {
            page.releaseReference();
            int pageNumber = page.getPageNumber();
            while (!Thread.currentThread().isInterrupted())
            {
                page.tryWriteEof();
                if (((int) CURRENT_PAGE_NUMBER_VH.get(this)) > pageNumber)
                {
                    // another write has won, and will allocate a new page
                    while ((((Page) CURRENT_PAGE_VH.get(this)).getPageNumber() == pageNumber))
                    {
                        Thread.yield();
                    }

                    break;
                }

                if (CURRENT_PAGE_NUMBER_VH.compareAndSet(this, pageNumber, pageNumber + 1))
                {
                    page.releaseReference();
                    // this thread won, allocate a new page
                    if (pageIndex.isPageCreated(pageNumber + 1))
                    {
                        CURRENT_PAGE_VH.setRelease(this, getPage(pageNumber + 1));
                    }
                    else
                    {
                        CURRENT_PAGE_VH.setRelease(this, allocator.safelyAllocatePage(pageNumber + 1));
                    }
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
            final Path pagePath = Filenames.forPageNumber(pageNumber, path);
            return Files.exists(pagePath);
        }
        return pageIndex.isPageCreated(pageNumber);
    }

    /**
     * Load a page from the page-cache.
     * <p>
     * Upon return from this method, the resulting <code>Page</code> will have had its reference count
     * incremented by 1. It is the responsibility of the caller to invoke the
     * <code>releaseReference()</code> method after the <code>Page</code> is no longer required.
     *
     * @param pageNumber the number of an existing page
     * @return the {@link Page}
     */
    public Page getPage(final int pageNumber)
    {
        return loadedPageCache.acquire(pageNumber);
    }

    Page allocate(final int pageNumber)
    {
        allocator.safelyAllocatePage(pageNumber).releaseReference();
        return getPage(pageNumber);
    }

    public Slice slice(final int pageNumber, final int position, final int recordLength)
    {
        final Page page = getPage(pageNumber);
        final ByteBuffer buffer = page.slice(position, recordLength);
        final Slice slice = SLICE.get();
        slice.set(buffer, page);
        return slice;
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

    public Unmapper getUnmapper()
    {
        return unmapper;
    }

    /**
     * Create a page-cache in the specified directory
     *
     * @param path     file-system path in which to store data
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

}