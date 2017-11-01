package com.aitusoftware.transport.buffer;

import java.util.function.IntFunction;

final class LoadedPageCache
{
    private static final int CACHED_PAGE_COUNT = 32;
    private final int indexMask;
    private final CachedPage[] cachedPages;
    private final IntFunction<Page> pageAllocator;

    LoadedPageCache(final PageAllocator allocator)
    {
        this(allocator::loadExisting, CACHED_PAGE_COUNT);
    }

    LoadedPageCache(final IntFunction<Page> allocator, final int cacheSize)
    {
        this.pageAllocator = allocator;
        this.indexMask = cacheSize - 1;
        cachedPages = new CachedPage[cacheSize];

    }

    Page acquire(final int pageNumber)
    {
        final int cachedPageIndex = toCachedPageIndex(pageNumber);
        CachedPage cachedPage = cachedPages[cachedPageIndex];
        if (cachedPage != null &&
                cachedPage.page != null &&
                cachedPage.page.claimReference())
        {
            final Page page = cachedPage.page;
            if (page.getPageNumber() == pageNumber)
            {
                cachedPages[cachedPageIndex] = cachedPage;
                return page;
            }
            else
            {
                cachedPage.page.releaseReference();
            }
        }
        else
        {
            cachedPage = new CachedPage();
        }

        final Page existing = pageAllocator.apply(pageNumber);
        cachedPage.page = existing;
        cachedPages[cachedPageIndex] = cachedPage;

        return existing;
    }

    private int toCachedPageIndex(final int pageNumber)
    {
        return pageNumber & indexMask;
    }

    private static final class CachedPage
    {
        private Page page;
    }
}