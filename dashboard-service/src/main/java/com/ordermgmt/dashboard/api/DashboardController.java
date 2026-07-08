package com.ordermgmt.dashboard.api;

import com.ordermgmt.dashboard.metrics.DashboardSnapshot;
import com.ordermgmt.dashboard.metrics.DashboardSnapshotService;
import com.ordermgmt.dashboard.stream.DashboardBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardBroadcaster broadcaster;
    private final DashboardSnapshotService snapshotService;

    public DashboardController(DashboardBroadcaster broadcaster, DashboardSnapshotService snapshotService) {
        this.broadcaster = broadcaster;
        this.snapshotService = snapshotService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DashboardSnapshot> stream() {
        return broadcaster.stream();
    }

    @GetMapping("/snapshot")
    public DashboardSnapshot snapshot() {
        return snapshotService.snapshot();
    }
}
