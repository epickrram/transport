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
package com.aitusoftware.transport.threads;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IdlersTest
{
    @Test
    public void shouldCreateFromString()
    {
        assertThat(Idlers.forString("BUSY_SPIN").getClass(), is(equalTo(BusySpinIdler.class)));
        assertThat(Idlers.forString("YIELDING").getClass(), is(equalTo(YieldingIdler.class)));
        assertThat(Idlers.forString("STATIC,1,MILLISECONDS").getClass(), is(equalTo(StaticPausingIdler.class)));
        assertThat(Idlers.forString("ADAPTIVE,1,MILLISECONDS").getClass(), is(equalTo(AdaptiveIdler.class)));
    }
}