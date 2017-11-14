package com.aitusoftware.transport.buffer;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.IntFunction;

final class LoadedPageCache
{
    private static final int CACHED_PAGE_COUNT = 32;
    private final int indexMask;
    private final AtomicReferenceArray<Page> cachedPages;
    private final IntFunction<Page> pageAllocator;

    LoadedPageCache(final PageAllocator allocator)
    {
        this(allocator::loadExisting, CACHED_PAGE_COUNT);
    }

    LoadedPageCache(final IntFunction<Page> allocator, final int cacheSize)
    {
        if (Integer.bitCount(cacheSize) != 1)
        {
            throw new IllegalArgumentException("cacheSize must be a power of two");
        }
        this.pageAllocator = allocator;
        this.indexMask = cacheSize - 1;
        cachedPages = new AtomicReferenceArray<>(cacheSize);

    }

    Page acquire(final int pageNumber)
    {
        final int cachedPageIndex = toCachedPageIndex(pageNumber);
        Page cachedPage = cachedPages.get(cachedPageIndex);
        if (cachedPage != null && cachedPage.getPageNumber() == pageNumber)
        {
            if (!cachedPage.claimReference())
            {
                return acquire(pageNumber);
            }
            return cachedPage;
        }

        final Page existing = pageAllocator.apply(pageNumber);
        if (cachedPage != null && cachedPages.compareAndSet(cachedPageIndex, cachedPage, null))
        {
            cachedPage.releaseReference();
        }
        if (!existing.claimReference())
        {
            return acquire(pageNumber);
        }
        cachedPages.set(cachedPageIndex, existing);

        return existing;
    }

    private int toCachedPageIndex(final int pageNumber)
    {
        return pageNumber & indexMask;
    }
}