package com.ordermgmt.dashboard.metrics;

import com.ordermgmt.dashboard.loadgen.LoadGeneratorStats;
import com.ordermgmt.dashboard.loadgen.LoadGeneratorStatsPoller;
import org.springframework.stereotype.Component;

/**
 * Assembles the final broadcast-ready {@link DashboardSnapshot} from the two
 * independent sources that feed it: the Kafka-driven pipeline aggregator,
 * and the load generator's own submission counters (polled separately,
 * since a 409-rejected duplicate order never touches Kafka at all).
 */
@Component
public class DashboardSnapshotService {

    private final DashboardMetricsAggregator aggregator;
    private final LoadGeneratorStatsPoller loadGeneratorStats;

    public DashboardSnapshotService(DashboardMetricsAggregator aggregator, LoadGeneratorStatsPoller loadGeneratorStats) {
        this.aggregator = aggregator;
        this.loadGeneratorStats = loadGeneratorStats;
    }

    public DashboardSnapshot snapshot() {
        DashboardSnapshot pipeline = aggregator.snapshot();
        LoadGeneratorStats loadGen = loadGeneratorStats.current();
        return new DashboardSnapshot(
                pipeline.totalOrdersPlaced(),
                pipeline.activeOrders(),
                pipeline.stateDistribution(),
                pipeline.throughputPerMinute(),
                pipeline.dlqCount(),
                pipeline.stuckOrders(),
                loadGen.accepted(),
                loadGen.duplicateRejected(),
                loadGen.errors(),
                pipeline.generatedAt());
    }
}
