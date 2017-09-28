package com.aitusoftware.transport.buffer;

public final class PageHeader {
    static final int HEADER_SIZE = 64 * 2;
    private static final int MAX_POSITION_DATA_OFFSET = 0;
    private static final int NUMBER_OF_POSITION_RECORDS = 4;
    private static final int POSITION_RECORD_SIZE = 8;
    private static final long CACHE_LINE_SIZE = 64L;
    private static final long CACHE_LINE_MASK = CACHE_LINE_SIZE - 1L;

    private final Slab slab;

    public PageHeader(final Slab slab) {
        this.slab = slab;
    }

    void updateMaxPosition(final long position) {

        long alignedPosition = getAlignedPosition(position);
        final int positionRecordSlot = (int) (alignedPosition & (NUMBER_OF_POSITION_RECORDS - 1));
        final int recordOffset = getRecordOffset(positionRecordSlot);
        long currentPosition;
        while ((currentPosition = slab.getLongVolatile(recordOffset)) < alignedPosition) {
            slab.compareAndSetLong(recordOffset, currentPosition, alignedPosition);
        }
    }

    private static long getAlignedPosition(final long position)
    {
        long alignedPosition = position;
        if ((position & CACHE_LINE_MASK) != 0L)
        {
            alignedPosition += CACHE_LINE_SIZE - (position & CACHE_LINE_MASK);
        }
        return alignedPosition;
    }

    long nextAvailablePosition() {
        long maxPosition = 0;
        for (int i = 0; i < NUMBER_OF_POSITION_RECORDS; i++) {
            maxPosition = Math.max(slab.getLongVolatile(getRecordOffset(i)), maxPosition);
        }

        return maxPosition;
    }

    @Override
    public String toString()
    {
        final StringBuilder buffer = new StringBuilder();

        buffer.append('\n');
        for (int i = 0; i < NUMBER_OF_POSITION_RECORDS; i++)
        {
            buffer.append(i).append(slab.getLongVolatile(getRecordOffset(i))).append('\n');
        }

        return buffer.toString();
    }

    private static int getRecordOffset(final int positionRecordSlot) {
        return MAX_POSITION_DATA_OFFSET + (positionRecordSlot * POSITION_RECORD_SIZE);
    }
}