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
