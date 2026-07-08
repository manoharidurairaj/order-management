package com.ordermgmt.pipeline.kafka;

import com.ordermgmt.common.domain.OrderFailedEvent;
import com.ordermgmt.common.domain.OrderStateChangedEvent;

public interface PipelineEventPublisher {

    void publishStateChanged(OrderStateChangedEvent event);

    void publishFailed(OrderFailedEvent event);
}
