package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.files.Buffers;
import com.aitusoftware.transport.files.Filenames;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

public final class PageIndex
{
    private static final int SLOTS = 128;
    private static final int SLOT_MASK = SLOTS - 1;
    private static final int SLOT_SIZE = 4;

    private final Slab slab;
    private final Path path;

    public static PageIndex forPageCache(final Path path) throws IOException
    {
        final PageIndex pageIndex = new PageIndex(SlabFactory.SLAB_FACTORY.
                createSlab(Buffers.map(
                        path.resolve("pages.idx"), false, SLOTS * SLOT_SIZE)), path);
        pageIndex.refresh();
        return pageIndex;
    }

    private PageIndex(final Slab slab, final Path path)
    {
        this.slab = slab;
        this.path = path;
    }

    void refresh()
    {
        final String[] files = path.toFile().
                list((dir, name) -> name.endsWith(Filenames.SUFFIX));
        if (files != null)
        {
            Arrays.sort(files, Comparator.comparingInt(Filenames::toPageNumber));
            for (String file : files)
            {
                onPageCreated(Filenames.toPageNumber(file));
            }
        }
    }

    void onPageCreated(final int pageNumber)
    {
        final int offset = toOffset(pageNumber);
        final int pageAtSlot = slab.getIntVolatile(offset);
        if (pageNumber > pageAtSlot)
        {
            slab.compareAndSetInt(offset, pageAtSlot, pageNumber);
        }
    }

    boolean isPageCreated(final int pageNumber)
    {
        final int offset = toOffset(pageNumber);
        return slab.getIntVolatile(offset) == pageNumber;
    }

    private static int toOffset(final int pageNumber)
    {
        return 4 * (pageNumber & SLOT_MASK);
    }
}