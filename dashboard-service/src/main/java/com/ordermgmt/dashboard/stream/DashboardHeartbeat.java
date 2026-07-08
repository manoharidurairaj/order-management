package com.ordermgmt.dashboard.stream;

import com.ordermgmt.dashboard.metrics.DashboardSnapshotService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pushes a fresh snapshot on a fixed cadence even when no Kafka event has
 * just arrived — otherwise throughput would never visibly decay and
 * stuck-order detection would only update when something else happened to
 * trigger a publish.
 */
@Component
public class DashboardHeartbeat {

    private final DashboardSnapshotService snapshotService;
    private final DashboardBroadcaster broadcaster;

    public DashboardHeartbeat(DashboardSnapshotService snapshotService, DashboardBroadcaster broadcaster) {
        this.snapshotService = snapshotService;
        this.broadcaster = broadcaster;
    }

    @Scheduled(fixedRateString = "${ordermgmt.dashboard.heartbeat-ms:2000}")
    public void tick() {
        broadcaster.publish(snapshotService.snapshot());
    }
}
