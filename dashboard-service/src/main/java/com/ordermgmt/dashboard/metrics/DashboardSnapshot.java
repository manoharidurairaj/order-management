package com.ordermgmt.dashboard.metrics;

import com.ordermgmt.common.domain.OrderState;

import java.time.Instant;
import java.util.Map;

public record DashboardSnapshot(
        long totalOrdersPlaced,
        long activeOrders,
        Map<OrderState, Long> stateDistribution,
        long throughputPerMinute,
        long dlqCount,
        long stuckOrders,
        long loadGenAccepted,
        long loadGenDuplicateRejected,
        long loadGenErrors,
        Instant generatedAt
) {
}
