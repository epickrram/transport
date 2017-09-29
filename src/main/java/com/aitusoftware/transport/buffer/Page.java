package com.aitusoftware.transport.buffer;

import java.nio.ByteBuffer;

import static com.aitusoftware.transport.buffer.Offsets.toPageOffset;

public final class Page
{
    private static final int CLAIMED_MARKER = 0b1000_0000_0000_0000_0000_0000_0000_0000;
    private static final int READY_MARKER = 0b1100_0000_0000_0000_0000_0000_0000_0000;
    private static final int MAX_DATA_LENGTH = 0b0100_0000_0000_0000_0000_0000_0000_0000 - 1;
    private static final int RECORD_LENGTH_MASK = 0b0011_1111_1111_1111_1111_1111_1111_1111;

    private final Slab slab;
    private final PageHeader pageHeader;
    private final int pageNumber;

    Page(final Slab slab, final int pageNumber)
    {
        this.slab = slab;
        pageHeader = new PageHeader(slab);
        this.pageNumber = pageNumber;
    }

    public WriteResult write(final ByteBuffer data)
    {
        final int remaining = data.remaining();
        if (remaining > MAX_DATA_LENGTH)
        {
            return WriteResult.MESSAGE_TOO_LARGE;
        }
        while (true)
        {
            final int position = pageHeader.nextAvailableWritePosition();

            if (position + remaining > availableDataLength())
            {
                return WriteResult.NOT_ENOUGH_SPACE;
            }
            if (claimPosition(position))
            {
                pageHeader.updateNextWritePosition(position + remaining + Record.HEADER_LENGTH);
                slab.copy(toPageOffset(position) + Record.HEADER_LENGTH, data);
                slab.writeOrderedInt(toPageOffset(position), READY_MARKER | remaining);
                return WriteResult.SUCCESS;
            }
        }
    }

    public void read(final int position, final ByteBuffer buffer)
    {
        slab.copyInto(toPageOffset(position) + Record.HEADER_LENGTH, buffer);
    }

    public int header(final int position)
    {
        return slab.getIntVolatile(toPageOffset(position));
    }

    int totalDataSize()
    {
        return slab.capacity() - PageHeader.HEADER_SIZE;
    }

    int nextAvailablePosition()
    {
        return pageHeader.nextAvailableWritePosition();
    }

    private long availableDataLength()
    {
        return slab.capacity() - PageHeader.HEADER_SIZE;
    }

    public static boolean isReady(final int recordHeader)
    {
        return (recordHeader & READY_MARKER) != 0;
    }

    public static int recordLength(final int recordHeader)
    {
        return recordHeader & MAX_DATA_LENGTH;
    }

    private boolean claimPosition(final int position)
    {
        return slab.compareAndSetInt(toPageOffset(position), 0, CLAIMED_MARKER);
    }

    @Override
    public String toString()
    {
        return "Page{" +
                "slab=" + slab +
                ", pageHeader=" + pageHeader +
                '}';
    }

    int getPageNumber()
    {
        return pageNumber;
    }
}