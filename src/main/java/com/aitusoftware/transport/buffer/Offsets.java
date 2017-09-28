package com.aitusoftware.transport.buffer;

public final class Offsets {
    private final int pageSize;
    private final int pageNumberShift;
    private final long pageOffsetMask;

    public Offsets(final int pageSize) {
        this.pageSize = pageSize;

        if (Integer.bitCount(pageSize) != 1) {
            throw new IllegalArgumentException("pageSize must be a power of two");
        }

        pageNumberShift = Integer.numberOfTrailingZeros(pageSize);
        pageOffsetMask = pageSize - 1;
    }

    int pageNumber(final long position) {
        return (int) (position >> pageNumberShift);
    }

    int pageOffset(final long position) {
        return (int) (position & pageOffsetMask);
    }
}