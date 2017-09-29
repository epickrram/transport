package com.aitusoftware.transport.buffer;

public final class PageHeader
{
    static final int HEADER_SIZE = 64 * 2;
    private static final int MAX_POSITION_DATA_OFFSET = 0;
    private static final int NUMBER_OF_POSITION_RECORDS = 4;
    private static final int POSITION_RECORD_SIZE = 8;

    private final Slab slab;

    public PageHeader(final Slab slab)
    {
        this.slab = slab;
    }

    void updateNextWritePosition(final int position)
    {
        final int positionRecordSlot = (position & (NUMBER_OF_POSITION_RECORDS - 1));
        final int recordOffset = getRecordOffset(positionRecordSlot);
        int currentPosition;
        while ((currentPosition = slab.getIntVolatile(recordOffset)) < position)
        {
            slab.compareAndSetInt(recordOffset, currentPosition, position);
        }
    }

    int nextAvailableWritePosition()
    {
        int maxPosition = 0;
        for (int i = 0; i < NUMBER_OF_POSITION_RECORDS; i++)
        {
            maxPosition = Math.max(slab.getIntVolatile(getRecordOffset(i)), maxPosition);
        }

        return Offsets.getAlignedPosition(maxPosition);
    }

    @Override
    public String toString()
    {
        final StringBuilder buffer = new StringBuilder();

        buffer.append('\n');
        for (int i = 0; i < NUMBER_OF_POSITION_RECORDS; i++)
        {
            buffer.append(i).append(": ").append(slab.getLongVolatile(getRecordOffset(i))).append('\n');
        }

        return buffer.toString();
    }

    private static int getRecordOffset(final int positionRecordSlot)
    {
        return MAX_POSITION_DATA_OFFSET + (positionRecordSlot * POSITION_RECORD_SIZE);
    }
}