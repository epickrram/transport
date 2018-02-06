/*
 * Copyright 2017 - 2018 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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