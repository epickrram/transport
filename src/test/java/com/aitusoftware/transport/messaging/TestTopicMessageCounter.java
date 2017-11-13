package com.aitusoftware.transport.messaging;

final class TestTopicMessageCounter implements TestTopic
{
    private volatile int messageCount;

    @Override
    public void say(final CharSequence message, final int counter)
    {
        messageCount++;
    }

    int getMessageCount()
    {
        return messageCount;
    }
}
