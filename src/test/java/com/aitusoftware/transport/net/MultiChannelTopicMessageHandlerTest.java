package com.aitusoftware.transport.net;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntFunction;

import static com.aitusoftware.transport.Action.executeQuietly;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

public class MultiChannelTopicMessageHandlerTest
{
    private static final byte[] PAYLOAD = new byte[] {(byte) 4, 3, 1, 1, 0};
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Integer> requestedIndices = new ArrayList<>();
    private ServerSocketChannel server;
    private MultiChannelTopicMessageHandler messageHandler;

    @Before
    public void setUp() throws Exception
    {
        server = ServerSocketChannel.open();
        server.bind(null);
        final TopicToChannelMapper mapper = new TopicToChannelMapper(createChannel());
        messageHandler = new MultiChannelTopicMessageHandler(mapper, 4);
    }

    @After
    public void tearDown() throws Exception
    {
        executeQuietly(server::close);
        executeQuietly(executor::shutdownNow);
    }

    @Test
    public void shouldSendData() throws InterruptedException, ExecutionException, TimeoutException
    {
        final Future<byte[]> receiverOne = startReceiver();
        final Future<byte[]> receiverTwo = startReceiver();
        final Future<byte[]> receiverThree = startReceiver();
        final Future<byte[]> receiverFour = startReceiver();

        messageHandler.onTopicMessage(17, ByteBuffer.wrap(PAYLOAD));
        messageHandler.onTopicMessage(17, ByteBuffer.wrap(PAYLOAD));
        messageHandler.onTopicMessage(17, ByteBuffer.wrap(PAYLOAD));
        messageHandler.onTopicMessage(17, ByteBuffer.wrap(PAYLOAD));

        assertArrayEquals(receiverOne.get(1, TimeUnit.SECONDS), PAYLOAD);
        assertArrayEquals(receiverTwo.get(1, TimeUnit.SECONDS), PAYLOAD);
        assertArrayEquals(receiverThree.get(1, TimeUnit.SECONDS), PAYLOAD);
        assertArrayEquals(receiverFour.get(1, TimeUnit.SECONDS), PAYLOAD);
        assertThat(requestedIndices, is(Arrays.asList(0, 1, 2, 3)));
    }

    private Future<byte[]> startReceiver()
    {
        return executor.submit(() -> {
            try
            {
                final SocketChannel client = server.accept();
                client.configureBlocking(true);
                final ByteBuffer buffer = ByteBuffer.allocateDirect(64);
                client.read(buffer);
                buffer.flip();
                final byte[] payload = new byte[buffer.getInt()];
                buffer.get(payload);
                return payload;
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        });
    }

    private IntFunction<SocketChannel> createChannel()
    {
        return i -> {
            requestedIndices.add(i);
            try
            {
                return SocketChannel.open(server.getLocalAddress());
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        };
    }

}