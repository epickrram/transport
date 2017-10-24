package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.PausingIdler;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.getInteger;

public final class Preloader
{
    private static final int PAGE_SIZE = getInteger("aitusoftware.transport.pageSize", 4096);
    private final PageCache pageCache;
    private final PageIndex pageIndex;
    private final ByteBuffer buffer = ByteBuffer.allocate(1);
    private final Idler idler = new PausingIdler(1, TimeUnit.MICROSECONDS);
    private boolean pageZeroLoaded = false;
    private int lastLoadedPage = -1;

    public Preloader(final PageCache pageCache)
    {
        this.pageCache = pageCache;
        this.pageIndex = pageCache.getPageIndex();
    }

    public void execute()
    {
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
                lastLoadedPage = highestPageNumber + 1;
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
        for (int i = 0; i < pageCache.getPageSize(); i += PAGE_SIZE)
        {
            buffer.clear();
            newPage.read(i, buffer);
        }
        newPage.releaseReference();
    }
}