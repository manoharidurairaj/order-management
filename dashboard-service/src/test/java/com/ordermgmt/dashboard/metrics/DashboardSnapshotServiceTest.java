package com.ordermgmt.dashboard.metrics;

import com.ordermgmt.common.domain.OrderStateChangedEvent;
import com.ordermgmt.dashboard.loadgen.LoadGeneratorStatsPoller;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static com.ordermgmt.common.domain.OrderState.PLACED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardSnapshotServiceTest {

    @Test
    void mergesPipelineAggregateWithLoadGeneratorStats() {
        DashboardMetricsAggregator aggregator = new DashboardMetricsAggregator(30);
        aggregator.onStateChanged(new OrderStateChangedEvent("order-1", null, PLACED, Instant.now(), Map.of()));

        // Never actually polls (no test network access), so it reports
        // LoadGeneratorStats.UNKNOWN (all zeros) — this exercises that the
        // pipeline-derived fields still come through correctly when merged
        // with that default.
        LoadGeneratorStatsPoller poller = new LoadGeneratorStatsPoller("http://localhost:0");
        DashboardSnapshotService service = new DashboardSnapshotService(aggregator, poller);

        DashboardSnapshot snapshot = service.snapshot();
        assertEquals(1, snapshot.totalOrdersPlaced());
        assertEquals(1, snapshot.activeOrders());
        assertEquals(1L, snapshot.stateDistribution().get(PLACED));
        assertEquals(0, snapshot.loadGenAccepted());
        assertEquals(0, snapshot.loadGenDuplicateRejected());
        assertEquals(0, snapshot.loadGenErrors());
    }
}
