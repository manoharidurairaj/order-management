package com.ordermgmt.dashboard.metrics;

import com.ordermgmt.common.domain.OrderState;
import com.ordermgmt.common.domain.OrderStateChangedEvent;
import com.ordermgmt.common.domain.OrderStateMachine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * All state lives in memory (not Redis) — the plan explicitly leaves this as
 * an open choice, and for a single-instance dashboard reading from Kafka,
 * local memory is the simpler option with nothing lost: on restart the
 * aggregates just rebuild from the live event stream rather than a
 * snapshot, which is fine for an operational dashboard.
 */
@Component
public class DashboardMetricsAggregator {

    private final Map<String, ActiveOrderSnapshot> activeOrders = new ConcurrentHashMap<>();
    private final Map<OrderState, LongAdder> terminalCounts = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<Instant> recentTransitions = new ConcurrentLinkedDeque<>();
    private final AtomicLong totalOrdersPlaced = new AtomicLong();
    private final AtomicLong dlqCount = new AtomicLong();
    private final Duration stuckThreshold;

    public DashboardMetricsAggregator(@Value("${ordermgmt.dashboard.stuck-threshold-seconds:30}") long stuckThresholdSeconds) {
        this.stuckThreshold = Duration.ofSeconds(stuckThresholdSeconds);
    }

    public void onStateChanged(OrderStateChangedEvent event) {
        Instant now = event.occurredAt();
        recentTransitions.addLast(now);
        trimOlderThan(recentTransitions, Duration.ofMinutes(1));

        if (event.fromState() == null) {
            totalOrdersPlaced.incrementAndGet();
        }

        OrderState to = event.toState();
        if (OrderStateMachine.isTerminal(to)) {
            activeOrders.remove(event.orderId());
            terminalCounts.computeIfAbsent(to, s -> new LongAdder()).increment();
        } else {
            activeOrders.put(event.orderId(), new ActiveOrderSnapshot(to, now));
        }
    }

    public void onOrderFailed() {
        dlqCount.incrementAndGet();
    }

    public DashboardSnapshot snapshot() {
        Instant now = Instant.now();

        Map<OrderState, Long> distribution = new EnumMap<>(OrderState.class);
        for (OrderState state : OrderState.values()) {
            distribution.put(state, 0L);
        }
        for (ActiveOrderSnapshot snapshot : activeOrders.values()) {
            distribution.merge(snapshot.state(), 1L, Long::sum);
        }
        terminalCounts.forEach((state, adder) -> distribution.put(state, adder.sum()));

        long stuck = activeOrders.values().stream()
                .filter(s -> Duration.between(s.since(), now).compareTo(stuckThreshold) > 0)
                .count();

        trimOlderThan(recentTransitions, Duration.ofMinutes(1));

        return new DashboardSnapshot(
                totalOrdersPlaced.get(),
                activeOrders.size(),
                distribution,
                recentTransitions.size(),
                dlqCount.get(),
                stuck,
                // Load-generator submission stats are outside this aggregator's
                // scope (it only knows about Kafka pipeline events) — filled in
                // by DashboardSnapshotService, which merges this with a live
                // poll of the load generator's own counters.
                0, 0, 0,
                now);
    }

    private void trimOlderThan(ConcurrentLinkedDeque<Instant> deque, Duration window) {
        Instant cutoff = Instant.now().minus(window);
        while (true) {
            Instant head = deque.peekFirst();
            if (head == null || head.isAfter(cutoff)) {
                break;
            }
            deque.pollFirst();
        }
    }
}
