package com.aitusoftware.transport.buffer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public enum Fixtures
{
    ;

    private static final Collection<Path> PATHS_TO_DELETE =
            new ConcurrentLinkedQueue<>();

    static
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Path path : PATHS_TO_DELETE)
            {
                recursiveDelete(path);
            }
        }));
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

    public static void writeMessages(final ByteBuffer buffer, final PageCache pageCache, final int messageCount)
    {
        for (int i = 0; i < messageCount; i++)
        {
            tagMessage(buffer, i);

            pageCache.append(buffer);
        }
    }

    public static boolean isValidMessage(final ByteBuffer buffer, final int messageIndex)
    {
        if (buffer.remaining() == 0)
        {
            return false;
        }
        for (int i = 0; i < buffer.remaining(); i++)
        {
            if (buffer.get(i) != (byte) messageIndex)
            {
                return false;
            }
        }

        return true;
    }

    private static void tagMessage(final ByteBuffer target, final int messageId)
    {
        target.clear();
        while (target.remaining() != 0)
        {
            target.put((byte) messageId);
        }
        target.flip();
    }
}
