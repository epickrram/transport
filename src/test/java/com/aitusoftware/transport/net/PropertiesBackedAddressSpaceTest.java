package com.aitusoftware.transport.net;

import com.aitusoftware.transport.integration.OrderNotifications;
import com.aitusoftware.transport.messaging.ExecutionReport;
import com.aitusoftware.transport.messaging.OrderDetails;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.WritableByteChannel;
import java.util.Properties;

import static java.util.List.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PropertiesBackedAddressSpaceTest
{
    private static final String EXECUTION_REPORT_FIRST_ADDRESS = "127.0.0.1:14768";
    private static final String EXECUTION_REPORT_SECOND_ADDRESS = "127.0.0.1:12345";
    private static final String EXECUTION_REPORT_ADDRESS_SPEC = String.format("%s,%s",
            EXECUTION_REPORT_FIRST_ADDRESS, EXECUTION_REPORT_SECOND_ADDRESS);
    private static final String ORDER_DETAILS_ADDRESS = "0.0.0.0:56748";
    private final Properties properties = new Properties();
    private final PropertiesBackedAddressSpace addressSpace =
            new PropertiesBackedAddressSpace(properties);

    @Before
    public void setUp()
    {
        properties.put(ExecutionReport.class.getName(), EXECUTION_REPORT_ADDRESS_SPEC);
        properties.put(OrderDetails.class.getName(), ORDER_DETAILS_ADDRESS);
    }

    @Test
    public void shouldResolveMultipleAddressesFromSpecifiedProperties()
    {
        assertThat(addressSpace.addressesOf(ExecutionReport.class),
                is(of(socketAddress(EXECUTION_REPORT_FIRST_ADDRESS),
                        socketAddress(EXECUTION_REPORT_SECOND_ADDRESS))));
    }

    @Test
    public void shouldResolveSingleAddressFromSpecifiedProperties()
    {
        assertThat(addressSpace.addressesOf(OrderDetails.class),
                is(of(socketAddress(ORDER_DETAILS_ADDRESS))));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldBlowUpWhenAccessingSingleAddressIfMultipleAddressesAreSpecified()
    {
        addressSpace.addressOf(ExecutionReport.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldBlowUpIfTopicClassIsNotSpecified()
    {
        addressSpace.addressesOf(OrderNotifications.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldBlowUpIfClassDoesNotDefineTopic()
    {
        addressSpace.addressesOf(WritableByteChannel.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotSupportHostOf()
    {
        addressSpace.hostOf(ExecutionReport.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotSupportPortOf()
    {
        addressSpace.portOf(ExecutionReport.class);
    }

    private SocketAddress socketAddress(final String spec)
    {
        final String[] tokens = spec.split(":");
        return new InetSocketAddress(tokens[0], Integer.parseInt(tokens[1]));
    }
}