package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.Fixtures;
import com.aitusoftware.transport.files.Filenames;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PageIndexTest
{
    private final Path path = Fixtures.tempDirectory();
    private PageIndex pageIndex;

    @Before
    public void setUp() throws Exception
    {
        pageIndex = PageIndex.forPageCache(path);
    }

    @Test
    public void shouldTrackHighestFileNumber() throws Exception
    {
        for (int i = 0; i < 20; i++)
        {
            Files.createFile(Filenames.forPageNumber(i, path));
        }

        pageIndex.refresh();

        for (int i = 0; i < 20; i++)
        {
            assertThat(pageIndex.isPageCreated(i), is(true));
        }

        assertThat(pageIndex.isPageCreated(21), is(false));

        pageIndex.onPageCreated(21);

        assertThat(pageIndex.isPageCreated(21), is(true));

        pageIndex.onPageCreated(22);

        assertThat(pageIndex.getHighestPageNumber(), is(22));
    }

    @Test
    public void shouldIndicateThatLowPageNumberIsNotPresent() throws Exception
    {
        for (int i = 0; i < PageIndex.SLOTS; i++)
        {
            Files.createFile(Filenames.forPageNumber(i, path));
        }

        pageIndex.refresh();

        assertThat(pageIndex.isPageCreated(0), is(true));
        assertThat(pageIndex.isPageCreated(PageIndex.SLOTS - 1), is(true));
        assertThat(pageIndex.isPageCreated(PageIndex.SLOTS), is(false));

        Files.createFile(Filenames.forPageNumber(PageIndex.SLOTS, path));

        pageIndex.refresh();

        assertThat(pageIndex.isPageCreated(0), is(false));
        assertThat(pageIndex.isPageCreated(PageIndex.SLOTS), is(true));
        assertThat(pageIndex.isLessThanLowestTrackedPageNumber(0), is(true));
    }
}