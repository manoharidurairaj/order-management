package com.ordermgmt.ingestion.kafka;

import com.ordermgmt.common.domain.OrderCancelRequestedEvent;
import com.ordermgmt.common.domain.OrderPlacedEvent;

/**
 * Order event publisher.
 */
public interface OrderEventPublisher {

    void publishOrderPlaced(OrderPlacedEvent event);

    void publishCancelRequested(OrderCancelRequestedEvent event);
}
