package com.ordermgmt.loadgen.emit;

import com.ordermgmt.loadgen.generator.GeneratedOrder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class IngestionClient {

    private static final Logger log = LoggerFactory.getLogger(IngestionClient.class);

    private final RestClient restClient;
    private final Counter accepted;
    private final Counter duplicateRejected;
    private final Counter errors;

    public IngestionClient(RestClient ingestionRestClient, MeterRegistry meterRegistry) {
        this.restClient = ingestionRestClient;
        this.accepted = Counter.builder("loadgen.orders.accepted").register(meterRegistry);
        this.duplicateRejected = Counter.builder("loadgen.orders.duplicate_rejected").register(meterRegistry);
        this.errors = Counter.builder("loadgen.orders.errors").register(meterRegistry);
    }

    public void submitOrder(GeneratedOrder order, String idempotencyKey) {
        try {
            restClient.post()
                    .uri("/api/orders")
                    .header("Idempotency-Key", idempotencyKey)
                    .body(order)
                    .retrieve()
                    .toBodilessEntity();
            accepted.increment();
        } catch (HttpClientErrorException.Conflict e) {
            // Expected outcome when we deliberately replay an idempotency
            // key — proves the ingestion dedup path live during a demo.
            duplicateRejected.increment();
        } catch (Exception e) {
            errors.increment();
            log.warn("Order submission failed: {}", e.toString());
        }
    }

    public long acceptedCount() {
        return (long) accepted.count();
    }

    public long duplicateRejectedCount() {
        return (long) duplicateRejected.count();
    }

    public long errorsCount() {
        return (long) errors.count();
    }
}
