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

public final class Offsets
{
    private static final long CACHE_LINE_SIZE = 64L;
    private static final long CACHE_LINE_MASK = CACHE_LINE_SIZE - 1L;

    private final int pageNumberShift;
    private final long pageOffsetMask;

    Offsets(final int pageSize)
    {
        if (Integer.bitCount(pageSize) != 1)
        {
            throw new IllegalArgumentException("pageSize must be a power of two");
        }

        pageNumberShift = Integer.numberOfTrailingZeros(pageSize);
        pageOffsetMask = pageSize - 1;
    }

    static int toPageOffset(final int position)
    {
        return position + PageHeader.HEADER_SIZE;
    }

    public static int getAlignedPosition(final int position)
    {
        int alignedPosition = position;
        if ((position & CACHE_LINE_MASK) != 0L)
        {
            alignedPosition += CACHE_LINE_SIZE - (position & CACHE_LINE_MASK);
        }
        return alignedPosition;
    }

    int pageNumber(final long position)
    {
        return (int) (position >> pageNumberShift);
    }

    int pageOffset(final long position)
    {
        return (int) (position & pageOffsetMask);
    }
}