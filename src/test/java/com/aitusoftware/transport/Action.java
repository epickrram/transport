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
package com.aitusoftware.transport;

public final class Action
{
    private Action() {}

    public static void executeQuietly(final ThrowingAction runnable)
    {
        try
        {
            runnable.execute();
        }
        catch(final Throwable t)
        {
            // ignore
        }
    }

    public static void executeQuietly(final ThrowingReturningAction runnable)
    {
        try
        {
            runnable.execute();
        }
        catch(final Throwable t)
        {
            // ignore
        }
    }

    public interface ThrowingAction
    {
        void execute() throws Throwable;
    }

    public interface ThrowingReturningAction
    {
        Object execute() throws Throwable;
    }
}
