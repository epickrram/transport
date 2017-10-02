package com.aitusoftware.transport.messaging.proxy;


import com.aitusoftware.transport.messaging.Topic;

@Topic
interface TestTopic
{
    void say(final CharSequence message, final int counter);
}
