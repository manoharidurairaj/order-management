package com.ordermgmt.dashboard.stream;

import com.ordermgmt.dashboard.metrics.DashboardSnapshot;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * {@code replay().latest()} means a browser opening the dashboard mid-rush
 * gets the current numbers immediately, not an empty screen until the next
 * Kafka event happens to arrive.
 */
@Component
public class DashboardBroadcaster {

    private final Sinks.Many<DashboardSnapshot> sink = Sinks.many().replay().latest();

    public void publish(DashboardSnapshot snapshot) {
        sink.tryEmitNext(snapshot);
    }

    public Flux<DashboardSnapshot> stream() {
        return sink.asFlux();
    }
}
