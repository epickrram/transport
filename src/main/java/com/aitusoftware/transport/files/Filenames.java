package com.aitusoftware.transport.files;

import java.nio.file.Path;

public enum Filenames
{
    FILENAMES;

    public static Path forPageNumber(final int pageNumber, final Path path)
    {
        return path.resolve(String.format("%1d.trx", pageNumber));
    }
}
