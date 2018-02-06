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