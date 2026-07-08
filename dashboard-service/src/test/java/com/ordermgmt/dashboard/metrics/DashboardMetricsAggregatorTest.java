package com.ordermgmt.dashboard.metrics;

import com.ordermgmt.common.domain.OrderStateChangedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static com.ordermgmt.common.domain.OrderState.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardMetricsAggregatorTest {

    @Test
    void placedEventIncrementsTotalAndActiveCounts() {
        DashboardMetricsAggregator aggregator = new DashboardMetricsAggregator(30);

        aggregator.onStateChanged(new OrderStateChangedEvent("order-1", null, PLACED, Instant.now(), Map.of()));

        DashboardSnapshot snapshot = aggregator.snapshot();
        assertEquals(1, snapshot.totalOrdersPlaced());
        assertEquals(1, snapshot.activeOrders());
        assertEquals(1L, snapshot.stateDistribution().get(PLACED));
    }

    @Test
    void terminalTransitionMovesOrderOutOfActiveCount() {
        DashboardMetricsAggregator aggregator = new DashboardMetricsAggregator(30);

        aggregator.onStateChanged(new OrderStateChangedEvent("order-1", null, PLACED, Instant.now(), Map.of()));
        aggregator.onStateChanged(new OrderStateChangedEvent("order-1", PLACED, CONFIRMED, Instant.now(), Map.of()));
        aggregator.onStateChanged(new OrderStateChangedEvent("order-1", CONFIRMED, CANCELLED, Instant.now(), Map.of()));

        DashboardSnapshot snapshot = aggregator.snapshot();
        assertEquals(0, snapshot.activeOrders());
        assertEquals(1L, snapshot.stateDistribution().get(CANCELLED));
        assertEquals(0L, snapshot.stateDistribution().get(CONFIRMED));
    }

    @Test
    void dlqEventsIncrementDlqCountIndependentlyOfStateDistribution() {
        DashboardMetricsAggregator aggregator = new DashboardMetricsAggregator(30);

        aggregator.onOrderFailed();
        aggregator.onOrderFailed();

        assertEquals(2, aggregator.snapshot().dlqCount());
    }

    @Test
    void ordersOlderThanThresholdAreCountedAsStuck() throws InterruptedException {
        DashboardMetricsAggregator aggregator = new DashboardMetricsAggregator(0);

        aggregator.onStateChanged(new OrderStateChangedEvent("order-1", null, PLACED, Instant.now(), Map.of()));
        Thread.sleep(5);

        assertTrue(aggregator.snapshot().stuckOrders() >= 1);
    }
}
