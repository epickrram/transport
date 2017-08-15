package com.aitusoftware.transport.buffer;

import java.nio.ByteBuffer;

public final class Page {
    private static final int CLAIMED_MARKER =  0b1000_0000_0000_0000_0000_0000_0000_0000;
    private static final int READY_MARKER =    0b1100_0000_0000_0000_0000_0000_0000_0000;
    private static final int MAX_DATA_LENGTH = 0b0100_0000_0000_0000_0000_0000_0000_0000 - 1;
    private final Slab slab;
    private final PageHeader pageHeader;

    public Page(final Slab slab) {
        this.slab = slab;
        pageHeader = new PageHeader(slab);
    }

    public void write(final ByteBuffer data) {
        final int remaining = data.remaining();
        if (remaining > MAX_DATA_LENGTH) {
            throw new IllegalArgumentException(String.format("Record size cannot exceed %db",
                    MAX_DATA_LENGTH));
        }
        while (true) {
            final long position = pageHeader.nextAvailablePosition();
            if (claimPosition(position)) {
                slab.copy((int) position + Record.HEADER_LENGTH, data);
                slab.writeOrderedInt((int) position, READY_MARKER | remaining);
                pageHeader.updateMaxPosition(position + remaining + Record.HEADER_LENGTH);
                break;
            }
        }
    }

    private static boolean isReady(final int recordHeader) {
        return (recordHeader & READY_MARKER) != 0;
    }

    private boolean claimPosition(final long position) {
        return slab.compareAndSetInt((int) position, 0, CLAIMED_MARKER);
    }
}