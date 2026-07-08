package com.ordermgmt.loadgen.state;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Load generator states — a steady baseline rate, an optional time-boxed burst
 * multiplier, and an optional total-order cap that auto-stops the baseline
 * once reached.
 */
@Component
public class LoadGeneratorState {

    private record Burst(double multiplier, Instant until) {
    }

    private final AtomicReference<Double> baselineOrdersPerSecond;
    private final AtomicReference<Burst> activeBurst = new AtomicReference<>();
    private final AtomicReference<Long> maxOrders = new AtomicReference<>();
    private final AtomicLong ordersSubmitted = new AtomicLong();

    public LoadGeneratorState(@Value("${ordermgmt.load.baseline-rate:2.0}") double defaultBaselineRate) {
        this.baselineOrdersPerSecond = new AtomicReference<>(defaultBaselineRate);
    }

    public void setBaselineRate(double ordersPerSecond) {
        baselineOrdersPerSecond.set(Math.max(0, ordersPerSecond));
    }

    /**
     * Sets the total-order cap for the current rate ({@code null} means
     * unlimited) and resets the submitted-count back to zero — each call to
     * {@code /load/rate} defines a fresh budget rather than adding to a
     * stale one.
     */
    public void setMaxOrders(Long limit) {
        maxOrders.set(limit);
        ordersSubmitted.set(0);
    }

    public void startBurst(double multiplier, long durationSeconds) {
        activeBurst.set(new Burst(Math.max(1.0, multiplier), Instant.now().plusSeconds(durationSeconds)));
    }

    public void clearBurst() {
        activeBurst.set(null);
    }

    public double baselineRate() {
        return baselineOrdersPerSecond.get();
    }

    public Long maxOrders() {
        return maxOrders.get();
    }

    public long ordersSubmitted() {
        return ordersSubmitted.get();
    }

    public boolean isBursting() {
        Burst burst = activeBurst.get();
        return burst != null && Instant.now().isBefore(burst.until());
    }

    public double effectiveRate() {
        Burst burst = activeBurst.get();
        if (burst != null && Instant.now().isBefore(burst.until())) {
            return baselineRate() * burst.multiplier();
        }
        if (burst != null) {
            activeBurst.compareAndSet(burst, null);
        }
        return baselineRate();
    }

    /**
     * Caps {@code requested} to whatever's left of the configured
     * {@code maxOrders} budget (returns {@code requested} unchanged if no
     * limit is set), and auto-stops the baseline rate the moment the budget
     * is exhausted so a demo can never run past the total it asked for.
     */
    public synchronized int reserveEmitBudget(int requested) {
        Long limit = maxOrders.get();
        if (limit == null || requested <= 0) {
            return requested;
        }

        long remaining = limit - ordersSubmitted.get();
        if (remaining <= 0) {
            setBaselineRate(0);
            return 0;
        }

        int allowed = (int) Math.min(requested, remaining);
        if (ordersSubmitted.addAndGet(allowed) >= limit) {
            setBaselineRate(0);
        }
        return allowed;
    }
}
