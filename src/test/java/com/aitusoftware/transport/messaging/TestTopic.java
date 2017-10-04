package com.aitusoftware.transport.messaging;

@Topic
public interface TestTopic
{
    void say(final CharSequence message, final int counter);
}
