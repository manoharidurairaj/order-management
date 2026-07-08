package com.ordermgmt.dashboard.metrics;

import com.ordermgmt.common.domain.OrderState;

import java.time.Instant;

record ActiveOrderSnapshot(OrderState state, Instant since) {
}
