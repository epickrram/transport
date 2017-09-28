package com.aitusoftware.transport.buffer;

import java.nio.ByteBuffer;

public final class Page {
    private static final int CLAIMED_MARKER =     0b1000_0000_0000_0000_0000_0000_0000_0000;
    private static final int READY_MARKER =       0b1100_0000_0000_0000_0000_0000_0000_0000;
    private static final int MAX_DATA_LENGTH =    0b0100_0000_0000_0000_0000_0000_0000_0000 - 1;
    private static final int RECORD_LENGTH_MASK = 0b0011_1111_1111_1111_1111_1111_1111_1111;

    private final Slab slab;
    private final PageHeader pageHeader;

    public Page(final Slab slab) {
        this.slab = slab;
        pageHeader = new PageHeader(slab);
    }

    public WriteResult write(final ByteBuffer data) {
        final int remaining = data.remaining();
        if (remaining > MAX_DATA_LENGTH) {
            return WriteResult.MESSAGE_TOO_LARGE;
        }
        while (true) {
            final long position = pageHeader.nextAvailablePosition();

            if (position + remaining > availableDataLength()) {
                return WriteResult.NOT_ENOUGH_SPACE;
            }
            if (claimPosition(position)) {
                slab.copy(toPageOffset(position) + Record.HEADER_LENGTH, data);
                slab.writeOrderedInt(toPageOffset(position), READY_MARKER | remaining);
                pageHeader.updateMaxPosition(position + remaining + Record.HEADER_LENGTH);
                return WriteResult.SUCCESS;
            }
        }
    }

    private long availableDataLength() {
        return slab.capacity() - PageHeader.HEADER_SIZE;
    }

    public void read(final long position, final ByteBuffer buffer) {
        slab.copyInto(toPageOffset(position) + Record.HEADER_LENGTH, buffer);
    }

    int header(final long position) {
        return slab.getIntVolatile(toPageOffset(position));
    }

    static boolean isReady(final int recordHeader) {
        return (recordHeader & READY_MARKER) != 0;
    }

    static int recordLength(final int recordHeader) {
        return recordHeader & MAX_DATA_LENGTH;
    }

    private int toPageOffset(final long position) {
        return (int) position + PageHeader.HEADER_SIZE;
    }

    private boolean claimPosition(final long position) {
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
}