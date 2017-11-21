package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.getInteger;

public final class Preloader
{
    private static final int PAGE_SIZE = getInteger("aitusoftware.transport.pageSize", 64);
    private static final String THREAD_NAME = "transport-preloader";

    private final PageCache pageCache;
    private final PageIndex pageIndex;
    private final ByteBuffer buffer = ByteBuffer.allocate(1);
    private final Idler idler = Idlers.staticPause(1, TimeUnit.MICROSECONDS);
    private boolean pageZeroLoaded = false;
    private int lastLoadedPage = -1;

    public Preloader(final PageCache pageCache)
    {
        this.pageCache = pageCache;
        this.pageIndex = pageCache.getPageIndex();
    }

    public void execute()
    {
        Thread.currentThread().setName(THREAD_NAME);
        while (!Thread.currentThread().isInterrupted())
        {
            final int highestPageNumber = pageIndex.getHighestPageNumber();
            if (highestPageNumber == 0 && !pageZeroLoaded)
            {
                preloadPage(0);
                pageZeroLoaded = true;
            }

            final Page page = pageCache.getPage(highestPageNumber);
            final int position = page.nextAvailablePosition();
            page.releaseReference();
            if (position != 0 && highestPageNumber > lastLoadedPage)
            {
                preloadPage(highestPageNumber + 1);
                preloadPage(highestPageNumber + 2);
                preloadPage(highestPageNumber + 3);
                preloadPage(highestPageNumber + 4);
                preloadPage(highestPageNumber + 5);
                lastLoadedPage = highestPageNumber + 2;
            }
            else
            {
                idler.idle();
            }
        }
    }

    private void preloadPage(final int pageNumber)
    {
        final Page newPage = pageCache.allocate(pageNumber);
        for (int i = 0; i < pageCache.getPageSize() - 32; i += PAGE_SIZE)
        {
            buffer.clear();
            newPage.read(i, buffer);
        }
        newPage.releaseReference();
    }
}