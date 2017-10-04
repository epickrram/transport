package com.aitusoftware.transport.messaging;

@Topic
public interface OtherTopic
{
    void testParams(final boolean truth, final byte tByte, final short tShort,
                    final int tInt, final float tFloat, final long tLong,
                    final double tDouble, final CharSequence zeroCopy,
                    @HeapAllocated final CharSequence heapCopy);
}