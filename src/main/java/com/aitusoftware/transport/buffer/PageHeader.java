package com.aitusoftware.transport.buffer;

public final class PageHeader {
    static final int HEADER_SIZE = 64 * 2;
    private static final int MAX_POSITION_DATA_OFFSET = 0;
    private static final int NUMBER_OF_POSITION_RECORDS = 4;
    private static final int POSITION_RECORD_SIZE = 8;

    private final Slab slab;

    public PageHeader(final Slab slab) {
        this.slab = slab;
    }

    void updateMaxPosition(final long position) {
        final int positionRecordSlot = (int) (position & (NUMBER_OF_POSITION_RECORDS - 1));
        final int recordOffset = getRecordOffset(positionRecordSlot);
        long currentPosition;
        while ((currentPosition = slab.getLongVolatile(recordOffset)) < position) {
            slab.compareAndSetLong(recordOffset, currentPosition, position);
        }
    }

    long nextAvailablePosition() {
        long maxPosition = 0;
        for (int i = 0; i < NUMBER_OF_POSITION_RECORDS; i++) {
            maxPosition = Math.max(slab.getLongVolatile(getRecordOffset(i)), maxPosition);
        }

        return maxPosition;
    }

    private static int getRecordOffset(final int positionRecordSlot) {
        return MAX_POSITION_DATA_OFFSET + (positionRecordSlot * POSITION_RECORD_SIZE);
    }
}