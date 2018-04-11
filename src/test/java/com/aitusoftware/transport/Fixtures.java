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
package com.aitusoftware.transport;

import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.buffer.WritableRecord;
import com.aitusoftware.transport.factory.IdlerConfig;
import com.aitusoftware.transport.threads.Idler;
import com.aitusoftware.transport.threads.Idlers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

public final class Fixtures
{
    private static final Collection<Path> PATHS_TO_DELETE =
            new ConcurrentLinkedQueue<>();
    private static final Idler TEST_IDLER = Idlers.staticPause(1, TimeUnit.MILLISECONDS);

    private Fixtures()
    {
    }

    static
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Path path : PATHS_TO_DELETE)
            {
                recursiveDelete(path);
            }
        }));
    }

    public static Function<Class<?>, Idler> testIdlerFactory()
    {
        return cls -> TEST_IDLER;
    }

    public static Idler testIdler()
    {
        return TEST_IDLER;
    }

    public static IdlerConfig testingIdlerConfig()
    {
        return new IdlerConfig()
        {
            @Override
            public Idler getInvokerIdler()
            {
                return TEST_IDLER;
            }

            @Override
            public Idler getPublisherIdler(final Class<?> topicDefinition)
            {
                return TEST_IDLER;
            }

            @Override
            public Idler getSubscriberIdler(final Class<?> topicDefinition)
            {
                return TEST_IDLER;
            }
        };
    }

    public static Path tempDirectory()
    {
        try
        {
            final Path tempDirectory = Files.createTempDirectory("transport");
            PATHS_TO_DELETE.add(tempDirectory);
            return tempDirectory;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeMessages(final ByteBuffer buffer, final PageCache pageCache, final int messageCount)
    {
        for (int i = 0; i < messageCount; i++)
        {
            buffer.clear();
            tagMessage(buffer, i);
            buffer.flip();

            final WritableRecord record = pageCache.acquireRecordBuffer(buffer.remaining());
            record.buffer().put(buffer);
            record.commit();
        }
    }

    public static void writeMessages(final int messageLength, final PageCache pageCache, final int messageCount)
    {
        for (int i = 0; i < messageCount; i++)
        {
            final WritableRecord record = pageCache.acquireRecordBuffer(messageLength);
            tagMessage(record.buffer(), i);
            record.commit();
        }
    }

    public static boolean isValidMessage(final ByteBuffer buffer, final int messageIndex)
    {
        final int messageLength = buffer.remaining();
        if (messageLength == 0)
        {
            return false;
        }
        for (int i = 0; i < messageLength; i++)
        {
            if (buffer.get() != (byte) messageIndex)
            {
                return false;
            }
        }

        return true;
    }

    private static void tagMessage(final ByteBuffer target, final int messageId)
    {
        while (target.remaining() != 0)
        {
            target.put((byte) messageId);
        }
    }

    private static void recursiveDelete(final Path path)
    {
        if (Files.isDirectory(path))
        {
            FileSystems.getDefault().getRootDirectories().forEach(r -> {
                if (r.equals(path))
                {
                    throw new IllegalArgumentException("Not deleting root directory: " + r);
                }
            });
            try (final Stream<Path> children = Files.list(path))
            {
                children.forEach(Fixtures::recursiveDelete);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
        try
        {
            Files.deleteIfExists(path);
        }
        catch (IOException e)
        {
            System.err.println("Failed to delete file: " + e.getMessage());
        }
    }
}
