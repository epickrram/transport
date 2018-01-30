package com.aitusoftware.transport.buffer;

import com.aitusoftware.transport.Fixtures;
import com.aitusoftware.transport.reader.RecordHandler;
import com.aitusoftware.transport.reader.StreamingReader;
import com.aitusoftware.transport.threads.Idlers;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PageCacheTest
{
    private static final int PAGE_SIZE = 4096;
    private static final int MESSAGE_SIZE = PAGE_SIZE / 23;
    private static final int PADDED_MESSAGE_SIZE = Offsets.getAlignedPosition(MESSAGE_SIZE);
    private static final int MESSAGES_PER_PAGE = PAGE_SIZE / PADDED_MESSAGE_SIZE;
    private static final int WASTED_PAGE_SPACE = PAGE_SIZE - (MESSAGES_PER_PAGE * PADDED_MESSAGE_SIZE);
    private static final int MESSAGE_COUNT = 128;
    private static final int PAGE_COUNT = 128 / 23;

    private final ByteBuffer message = ByteBuffer.allocate(MESSAGE_SIZE);
    private PageCache pageCache;
    private Path directory;

    @Before
    public void setUp() throws Exception
    {
        directory = Fixtures.tempDirectory();
        pageCache = PageCache.create(directory, PAGE_SIZE);
    }

    @Test
    public void shouldInitialiseFromExistingFiles() throws Exception
    {
        final int totalMessageCount = 1000;
        final int messageBatch = totalMessageCount / 2;
        for (int i = 0; i < messageBatch; i++)
        {
            final WritableRecord record = pageCache.acquireRecordBuffer(MESSAGE_SIZE);
            record.buffer().putInt(i);
            record.commit();
        }

        assertThat(Files.list(directory).count(), is(25L));

        final PageCache newPageCache = PageCache.create(directory, PAGE_SIZE);
        for (int i = messageBatch; i < totalMessageCount; i++)
        {
            final WritableRecord record = newPageCache.acquireRecordBuffer(MESSAGE_SIZE);
            record.buffer().putInt(i);
            record.commit();
        }

        final MessageValidator validator = new MessageValidator();
        final StreamingReader reader = new StreamingReader(newPageCache, validator, true, Idlers.staticPause(1, TimeUnit.MILLISECONDS));

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(reader::process);

        validator.waitForMessageCount(totalMessageCount);

        executor.shutdownNow();

        final int[] messages = validator.getMessages();
        assertThat(messages.length, is(totalMessageCount));
        int expected = 0;
        for (final int messageId : messages)
        {
            assertThat(messageId, is(expected));
            expected++;
        }
    }

    @Test
    public void shouldAppendDataOverSeveralPages() throws Exception
    {
        Fixtures.writeMessages(message, pageCache, MESSAGE_COUNT);

        assertThat(pageCache.estimateTotalLength(), is((long) MESSAGE_COUNT * PADDED_MESSAGE_SIZE + (PAGE_COUNT + 1) * WASTED_PAGE_SPACE));
    }

    @Test
    public void shouldAcquireZeroCopyBufferOverSeveralPages() throws Exception
    {
        Fixtures.writeMessages(MESSAGE_SIZE, pageCache, MESSAGE_COUNT);

        assertThat(pageCache.estimateTotalLength(), is((long) MESSAGE_COUNT * PADDED_MESSAGE_SIZE + (PAGE_COUNT + 1) * WASTED_PAGE_SPACE));
    }

    private static class MessageValidator implements RecordHandler
    {
        private final List<Integer> receivedMessageIds = new CopyOnWriteArrayList<>();

        @Override
        public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
        {
            receivedMessageIds.add(data.getInt());
        }

        void waitForMessageCount(final int totalMessageCount)
        {
            final long timeoutAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10L);
            while (System.currentTimeMillis() < timeoutAt)
            {
                if (receivedMessageIds.size() == totalMessageCount)
                {
                    return;
                }
            }
            fail(String.format("Only received %d messages, expecting %d", receivedMessageIds.size(), totalMessageCount));
        }


        int[] getMessages()
        {
            return receivedMessageIds.stream().mapToInt(Integer::intValue).toArray();
        }
    }
}