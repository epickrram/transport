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
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.aitusoftware.transport.buffer.PageIndex.forPageCache;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PageAllocatorTest
{

    private final Path path = Fixtures.tempDirectory();

    @Test
    public void shouldNotLeakFileHandles() throws Exception
    {
        final PageAllocator allocator = new PageAllocator(path, 512, forPageCache(path), new Unmapper());
        final List<Page> allocatedPages = new ArrayList<>();

        for (int i = 0; i < 10; i++)
        {
            allocatedPages.add(allocator.safelyAllocatePage(i));
        }

        allocatedPages.forEach(p -> {
            p.slice(0, 64).putLong(37L);
        });

        final List<String> postAllocationPageFiles = getPageFileHandles();

        assertThat("Page files still open: " + postAllocationPageFiles,
                postAllocationPageFiles.isEmpty(), is(true));
    }

    private List<String> getPageFileHandles() throws IOException
    {
        final List<String> pageFiles = new ArrayList<>();
        Files.list(Paths.get("/proc/self/fd")).
                map(toRealPath()).filter(p -> p.endsWith(Filenames.SUFFIX)).
                forEach(pageFiles::add);
        return pageFiles;
    }

    private static Function<Path, String> toRealPath()
    {
        return p -> {
            try
            {
                return p.toRealPath().toString();
            }
            catch (NoSuchFileException e)
            {
                return "Unknown: " + e.getFile();
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        };
    }
}