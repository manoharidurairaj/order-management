package com.ordermgmt.dashboard.loadgen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Best-effort poller for the load generator's own submission counters
 * (accepted / duplicate-rejected / errors). These live entirely outside
 * Kafka — a 409-rejected duplicate idempotency key never reaches the
 * pipeline — so polling the load generator directly is the only way the
 * live dashboard can show why "orders placed" can land below a requested
 * {@code maxOrders}. If the load generator is stopped or unreachable, the
 * last known values are kept (starting at zero) rather than failing the
 * whole dashboard snapshot.
 */
@Component
public class LoadGeneratorStatsPoller {

    private static final Logger log = LoggerFactory.getLogger(LoadGeneratorStatsPoller.class);

    private final WebClient webClient;
    private final AtomicReference<LoadGeneratorStats> latest = new AtomicReference<>(LoadGeneratorStats.UNKNOWN);

    public LoadGeneratorStatsPoller(
            @Value("${ordermgmt.load-generator.base-url:http://load-generator:8085}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Scheduled(fixedRateString = "${ordermgmt.dashboard.heartbeat-ms:2000}")
    public void poll() {
        try {
            LoadGeneratorStatusResponse status = webClient.get()
                    .uri("/load/status")
                    .retrieve()
                    .bodyToMono(LoadGeneratorStatusResponse.class)
                    .timeout(Duration.ofSeconds(2))
                    .block();
            if (status != null) {
                latest.set(new LoadGeneratorStats(
                        status.ordersAccepted(), status.ordersDuplicateRejected(), status.ordersErrors()));
            }
        } catch (Exception e) {
            log.debug("Load generator unreachable, keeping last known stats: {}", e.toString());
        }
    }

    public LoadGeneratorStats current() {
        return latest.get();
    }
}
