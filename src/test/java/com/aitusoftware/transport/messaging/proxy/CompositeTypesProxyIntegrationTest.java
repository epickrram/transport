package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.transport.buffer.Fixtures;
import com.aitusoftware.transport.buffer.PageCache;
import com.aitusoftware.transport.messaging.ExecutionReport;
import com.aitusoftware.transport.messaging.ExecutionReportBuilder;
import com.aitusoftware.transport.messaging.OrderDetails;
import com.aitusoftware.transport.messaging.OrderDetailsBuilder;
import com.aitusoftware.transport.reader.RecordHandler;
import com.aitusoftware.transport.reader.StreamingReader;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class CompositeTypesProxyIntegrationTest
{
    private final Path tempDir = Fixtures.tempDirectory();
    private PageCache pageCache;
    private PublisherFactory factory;
    private final SubscriberFactory subscriberFactory = new SubscriberFactory();

    @Before
    public void setUp() throws Exception
    {
        // 256/8192
        pageCache = PageCache.create(tempDir, 8192);
        factory = new PublisherFactory(pageCache);
    }

    @Ignore("buffer overflow in deserialisation")
    @Test
    public void shouldSendMessages() throws Exception
    {
        final CompositeTopic proxy = factory.getPublisherProxy(CompositeTopic.class);
        final List<ArgumentContainer> receivedMessages = new CopyOnWriteArrayList<>();
        final List<ArgumentContainer> sentMessages = new LinkedList<>();

        final Subscriber<CompositeTopic> subscriber =
                subscriberFactory.getSubscriber(CompositeTopic.class,
                        (id, orderDetails, executionReport, venueResponse, timestamp) -> {
                    receivedMessages.add(new ArgumentContainer(
                            id, orderDetails, executionReport, venueResponse, timestamp));
                });

        for (int i = 0; i < 50; i++)
        {
            final OrderDetailsBuilder orderDetails = new OrderDetailsBuilder();
            orderDetails.quantity(3 * i);
            orderDetails.price(5 * i);
            orderDetails.orderId(11 * i);
            orderDetails.setIdentifier("order_" + i);

            final ExecutionReportBuilder executionReport = new ExecutionReportBuilder();
            executionReport.isBid((i & 1) == 0);
            executionReport.orderId("exec_order_" + i);
            executionReport.statusMessage("status_" + i);
            executionReport.timestamp(13 * i);
            executionReport.quantity(17 * i);
            executionReport.price(19 * i);

            final String venueResponse = "response_" + i;
            final int timestamp = i * 7;
            proxy.sendData(i, orderDetails, executionReport, venueResponse, timestamp);

            sentMessages.add(new ArgumentContainer(i, orderDetails, executionReport, venueResponse, timestamp));
        }

        new StreamingReader(pageCache, new RecordHandler()
        {
            @Override
            public void onRecord(final ByteBuffer data, final int pageNumber, final int position)
            {
                data.getInt();
                subscriber.onRecord(data, pageNumber, position);
            }
        }, false, true).process();

        while (receivedMessages.size() != sentMessages.size())
        {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1L));
        }

        for (int i = 0; i < sentMessages.size(); i++)
        {
            final ArgumentContainer sent = sentMessages.get(i);
            final ArgumentContainer received = receivedMessages.get(i);

            assertThat(sent.id, is(received.id));
            assertThat(sent.timestamp, is(received.timestamp));
            assertThat(sent.venueResponse.toString(), is(received.venueResponse.toString()));

            assertThat(sent.orderDetails.getIdentifier().toString(),
                    is(received.orderDetails.getIdentifier().toString()));
            assertThat(sent.orderDetails.orderId(),
                    is(received.orderDetails.orderId()));
            assertThat(sent.orderDetails.price(),
                    is(received.orderDetails.price()));
            assertThat(sent.orderDetails.quantity(),
                    is(received.orderDetails.quantity()));

            assertThat(sent.executionReport.isBid(),
                    is(received.executionReport.isBid()));
            assertThat(sent.executionReport.orderId().toString(),
                    is(received.executionReport.orderId().toString()));
            assertThat(sent.executionReport.price(),
                    is(received.executionReport.price()));
            assertThat(sent.executionReport.quantity(),
                    is(received.executionReport.quantity()));
            assertThat(sent.executionReport.timestamp(),
                    is(received.executionReport.timestamp()));
            assertThat(sent.executionReport.statusMessage().toString(),
                    is(received.executionReport.statusMessage().toString()));
        }
    }

    private static final class ArgumentContainer
    {
        private final long id;
        private final OrderDetails orderDetails;
        private final ExecutionReport executionReport;
        private final CharSequence venueResponse;
        private final long timestamp;

        ArgumentContainer(final long id, final OrderDetails orderDetails, final ExecutionReport executionReport, final CharSequence venueResponse, final long timestamp)
        {
            this.id = id;
            this.orderDetails = orderDetails;
            this.executionReport = executionReport;
            this.venueResponse = venueResponse;
            this.timestamp = timestamp;
        }
    }
}
