package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.transport.reader.RecordHandler;

public interface Subscriber<T> extends RecordHandler
{
    int getTopicId();
}
