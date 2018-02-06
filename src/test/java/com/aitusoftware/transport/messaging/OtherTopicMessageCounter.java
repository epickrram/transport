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
package com.aitusoftware.transport.messaging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

final class OtherTopicMessageCounter implements OtherTopic
{
    private volatile int messageCount;

    @Override
    public void testParams(final boolean truth, final byte tByte, final short tShort,
                           final int tInt, final float tFloat, final long tLong,
                           final double tDouble, final CharSequence zeroCopy, final CharSequence heapCopy)
    {
        messageCount++;
        if (truth)
        {
            assertThat(tByte, is((byte) 5));
            assertThat(tShort, is((short) 7));
            assertThat(tInt, is(11));
            assertThat(tFloat, is(13.7f));
            assertThat(tLong, is(17L));
            assertThat(tDouble, is(19.37d));
            assertThat(zeroCopy.toString(), is("first"));
            assertThat(heapCopy.toString(), is("second"));
        }
        else
        {
            assertThat(tByte, is((byte) -5));
            assertThat(tShort, is((short) -7));
            assertThat(tInt, is(-11));
            assertThat(tFloat, is(Float.MAX_VALUE));
            assertThat(tLong, is(Long.MIN_VALUE));
            assertThat(tDouble, is(Double.POSITIVE_INFINITY));
            assertThat(zeroCopy.toString(), is("first"));
            assertThat(heapCopy.toString(), is("second"));
        }
    }

    int getMessageCount()
    {
        return messageCount;
    }
}
