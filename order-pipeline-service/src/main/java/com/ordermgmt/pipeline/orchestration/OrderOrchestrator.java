package com.ordermgmt.pipeline.orchestration;

import com.ordermgmt.common.domain.OrderCancelRequestedEvent;
import com.ordermgmt.common.domain.OrderPlacedEvent;

public interface OrderOrchestrator {

    void handlePlaced(OrderPlacedEvent event);

    void handleCancelRequested(OrderCancelRequestedEvent event);
}
