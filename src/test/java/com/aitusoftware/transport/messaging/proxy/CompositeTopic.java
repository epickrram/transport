package com.aitusoftware.transport.messaging.proxy;

import com.aitusoftware.transport.messaging.ExecutionReport;
import com.aitusoftware.transport.messaging.OrderDetails;
import com.aitusoftware.transport.messaging.Topic;

@Topic
public interface CompositeTopic
{
    void sendData(final long id, final OrderDetails orderDetails,
                  final ExecutionReport executionReport, final CharSequence venueResponse,
                  final long timestamp);
}
