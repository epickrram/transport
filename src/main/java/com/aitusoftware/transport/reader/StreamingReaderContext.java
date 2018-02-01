package com.aitusoftware.transport.reader;

import com.aitusoftware.transport.threads.SingleThreaded;

@SingleThreaded
public final class StreamingReaderContext
{
    private static final ThreadLocal<StreamingReaderContext> THREAD_LOCAL =
            ThreadLocal.withInitial(StreamingReaderContext::new);
    private int pageNumber;
    private int position;
    private long localMessageCount;

    public static StreamingReaderContext get()
    {
        return THREAD_LOCAL.get();
    }

    public int getPageNumber()
    {
        return pageNumber;
    }

    public int getPosition()
    {
        return position;
    }

    public long getMessageCount()
    {
        return localMessageCount;
    }

    void update(final int pageNumber, final int position, final long localMessageCount)
    {
        this.pageNumber = pageNumber;
        this.position = position;
        this.localMessageCount = localMessageCount;
    }

    void reset()
    {
        pageNumber = Integer.MIN_VALUE;
        position = Integer.MIN_VALUE;
        localMessageCount = Long.MIN_VALUE;
    }
}