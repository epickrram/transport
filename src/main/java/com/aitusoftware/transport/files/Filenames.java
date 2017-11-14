package com.aitusoftware.transport.files;

import java.nio.file.Path;

public final class Filenames
{
    private Filenames()
    {
    }

    public static final String SUFFIX = ".trx";

    public static Path forPageNumber(final int pageNumber, final Path path)
    {
        return path.resolve(formatPageNumber(pageNumber));
    }

    public static int toPageNumber(final String filename)
    {
        return Integer.parseInt(filename.substring(0, filename.indexOf('.')));
    }

    private static String formatPageNumber(final int pageNumber)
    {
        return String.format("%018d" + SUFFIX, pageNumber);
    }
}
