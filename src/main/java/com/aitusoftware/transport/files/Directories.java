package com.aitusoftware.transport.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Directories
{
    private Directories()
    {
    }

    public static void ensureDirectoryExists(final Path path)
    {
        if (Files.exists(path))
        {
            if (!Files.isDirectory(path))
            {
                throw new IllegalArgumentException(String.format(
                        "%s exists and is not a directory", path));
            }
            return;
        }

        try
        {
            Files.createDirectory(path);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
