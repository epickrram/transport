package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.memory.ReferenceCounter;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static com.aitusoftware.transport.buffer.Offsets.toPageOffset;

/**
 * Represents a fixed-size area of shared memory.
 */
public final class Page
{
    static final int CLAIMED_MARKER = 0b1000_0000_0000_0000_0000_0000_0000_0000;
    static final int READY_MARKER = 0b0100_0000_0000_0000_0000_0000_0000_0000;
    private static final int EOF_MARKER = 0b0010_0000_0000_0000_0000_0000_0000_0000;
    private static final int READY_MARKER_MASK = 0b0100_0000_0000_0000_0000_0000_0000_0000;

    private static final int MAX_DATA_LENGTH = 0b0010_0000_0000_0000_0000_0000_0000_0000 - 1;
    static final int ERR_MESSAGE_TOO_LARGE = -1;
    static final int ERR_NOT_ENOUGH_SPACE = -2;

    private final ThreadLocal<ByteBuffer> slice =
            ThreadLocal.withInitial(() -> null);

    private final Slab slab;
    private final PageHeader pageHeader;
    private final int pageNumber;
    private final Path pagePath;
    private final ReferenceCounter referenceCounter;

    Page(final Slab slab, final int pageNumber, final Path pagePath)
    {
        this.slab = slab;
        // TODO pageSize should be in header
        pageHeader = new PageHeader(slab);
        this.pageNumber = pageNumber;
        this.pagePath = pagePath;
        this.referenceCounter = new ReferenceCounter();
    }

    // visible for testing, consider for removal
    WriteResult write(final ByteBuffer data)
    {
        final int remaining = data.remaining();
        final int response = acquireSpaceInBuffer(remaining);

        if (response == ERR_MESSAGE_TOO_LARGE)
        {
            return WriteResult.MESSAGE_TOO_LARGE;
        }
        else if (response == ERR_NOT_ENOUGH_SPACE)
        {
            return WriteResult.NOT_ENOUGH_SPACE;
        }
        pageHeader.updateNextWritePosition(response + remaining + Record.HEADER_LENGTH);
        slab.copy(toPageOffset(response) + Record.HEADER_LENGTH, data);
        slab.writeOrderedInt(toPageOffset(response), READY_MARKER | remaining);
        return WriteResult.SUCCESS;
    }

    void writeReadyHeader(final int headerOffset, final int recordLength)
    {
        slab.writeOrderedInt(toPageOffset(headerOffset), READY_MARKER | recordLength);
    }

    int acquireSpaceInBuffer(final int remaining)
    {
        if (remaining > MAX_DATA_LENGTH)
        {
            return ERR_MESSAGE_TOO_LARGE;
        }
        while (true)
        {
            final int position = pageHeader.nextAvailableWritePosition();

            if (position + remaining + Record.HEADER_LENGTH > availableDataLength())
            {
                return ERR_NOT_ENOUGH_SPACE;
            }
            if ((slab.getIntVolatile(toPageOffset(position)) & EOF_MARKER) != 0)
            {
                return ERR_NOT_ENOUGH_SPACE;
            }
            if (claimPosition(position))
            {
                pageHeader.updateNextWritePosition(Offsets.getAlignedPosition(position + remaining + Record.HEADER_LENGTH));
                return position;
            }
        }
    }

    void tryWriteEof()
    {
        final int position = pageHeader.nextAvailableWritePosition();
        final int offset = toPageOffset(position);

        if (offset < slab.capacity() - 4)
        {
            slab.compareAndSetInt(offset, 0, EOF_MARKER);
        }
    }

    void read(final int position, final ByteBuffer buffer)
    {
        slab.copyInto(toPageOffset(position) + Record.HEADER_LENGTH, buffer);
    }

    ByteBuffer slice(final int position, final int recordLength)
    {
        final ByteBuffer slice = getSlice();

        final int newPosition = toPageOffset(position) + Record.HEADER_LENGTH;
        slice.clear();
        final int newLimit = newPosition + recordLength;
        if (newLimit > slab.capacity())
        {
            throw new IllegalArgumentException(String.format(
                    "Trying to set limit (%d) past slab capacity (%d); position: %d, length: %d, offset: %d",
                    newLimit, slab.capacity(), position, recordLength, newPosition));
        }
        slice.position(newPosition).limit(newLimit);

        return slice;
    }

    boolean claimReference()
    {
        return referenceCounter.claim();
    }

    public void releaseReference()
    {
        referenceCounter.release();
    }

    int referenceCount()
    {
        return referenceCounter.getReferenceCount();
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

    public static boolean isReady(final int recordHeader)
    {
        return (recordHeader & READY_MARKER_MASK) != 0;
    }

    public static boolean isEof(final int header)
    {
        return (header & EOF_MARKER) != 0;
    }

    public static int recordLength(final int recordHeader)
    {
        return recordHeader & MAX_DATA_LENGTH;
    }

    private long availableDataLength()
    {
        return slab.capacity() - PageHeader.HEADER_SIZE;
    }

    private boolean claimPosition(final int position)
    {
        return slab.compareAndSetInt(toPageOffset(position), 0, CLAIMED_MARKER);
    }

    @Override
    public String toString()
    {
        return "Page{" +
                "file=" + pagePath +
                ", slab=" + slab +
                ", pageHeader=" + pageHeader +
                '}';
    }

    int getPageNumber()
    {
        return pageNumber;
    }

    boolean acquireForCleanup()
    {
        if (referenceCounter.getReferenceCount() == 0 &&
                referenceCounter.lastClaimIsOlderThan(3, TimeUnit.SECONDS))
        {
            return referenceCounter.makeUnreachable();
        }
        return false;
    }

    void unmap()
    {
        slab.unmap();
    }

    private ByteBuffer getSlice()
    {
        ByteBuffer slice = this.slice.get();
        if (slice == null)
        {
            slice = slab.slice();
            this.slice.set(slice);
        }
        return slice;
    }
}