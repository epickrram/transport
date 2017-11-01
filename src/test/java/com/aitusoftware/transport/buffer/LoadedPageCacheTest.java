package com.aitusoftware.transport.buffer;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LoadedPageCacheTest
{
    private final LoadedPageCache cache = new LoadedPageCache(this::createPageFor, 4);
    private final Map<Integer, Integer> pageCreateCount = new HashMap<>();

    @Test
    public void shouldAllocateAndStorePage() throws Exception
    {
        final Page page = cache.acquire(1);

        assertThat(pageCreateCount.get(1), is(1));
        assertThat(pageCreateCount.size(), is(1));
    }

    @Test
    public void shouldOverwritePageWithClashingIndex() throws Exception
    {
        final Page page1 = cache.acquire(1);
        final Page page5 = cache.acquire(5);

        assertThat(pageCreateCount.get(1), is(1));
        assertThat(pageCreateCount.get(5), is(1));
        assertThat(pageCreateCount.size(), is(2));
    }

    private Page createPageFor(final int pageNumber)
    {
        final Integer existingCount = pageCreateCount.computeIfAbsent(pageNumber, i -> 0);
        pageCreateCount.put(pageNumber, existingCount + 1);
        return new Page(SlabFactory.SLAB_FACTORY.createSlab(ByteBuffer.allocate(64)), pageNumber,
                Paths.get(System.getProperty("java.io.tmpdir")));
    }
}