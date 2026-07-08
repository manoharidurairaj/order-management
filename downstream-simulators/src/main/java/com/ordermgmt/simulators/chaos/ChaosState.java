package com.ordermgmt.simulators.chaos;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutable, thread-safe home for the tunable slice of the chaos profile —
 * latency probability, 503 rate, 429 rate.
 */
@Component
public class ChaosState {

    private final long latencyMinMs;
    private final long latencyMaxMs;
    private final AtomicReference<Double> latencyProbability;
    private final AtomicReference<Double> rate503;
    private final AtomicReference<Double> rate429;

    public ChaosState(ChaosProperties initial) {
        this.latencyMinMs = initial.latency().minMs();
        this.latencyMaxMs = initial.latency().maxMs();
        this.latencyProbability = new AtomicReference<>(initial.latency().probability());
        this.rate503 = new AtomicReference<>(initial.rate503());
        this.rate429 = new AtomicReference<>(initial.rate429());
    }

    public void update(double latencyProbability, double rate503, double rate429) {
        this.latencyProbability.set(latencyProbability);
        this.rate503.set(rate503);
        this.rate429.set(rate429);
    }

    public ChaosProperties current() {
        return new ChaosProperties(
                new ChaosProperties.Latency(latencyProbability.get(), latencyMinMs, latencyMaxMs),
                rate503.get(),
                rate429.get());
    }
}
