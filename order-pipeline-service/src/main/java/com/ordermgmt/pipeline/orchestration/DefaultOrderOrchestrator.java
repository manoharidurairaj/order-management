package com.ordermgmt.pipeline.orchestration;

import com.ordermgmt.common.domain.*;
import com.ordermgmt.pipeline.client.CourierClient;
import com.ordermgmt.pipeline.client.DownstreamUnavailableException;
import com.ordermgmt.pipeline.client.RestaurantClient;
import com.ordermgmt.pipeline.kafka.PipelineEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static com.ordermgmt.common.domain.OrderState.*;

/**
 * Sequences each order through its lifecycle.
 */
@Component
public class DefaultOrderOrchestrator implements OrderOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DefaultOrderOrchestrator.class);

    private final OrderTransitionService transitions;
    private final RestaurantClient restaurantClient;
    private final CourierClient courierClient;
    private final PipelineEventPublisher eventPublisher;
    private final TaskScheduler taskScheduler;
    private final long minDeliveryDelayMs;
    private final long maxDeliveryDelayMs;
    private final long staleAfterMs;

    public DefaultOrderOrchestrator(
            OrderTransitionService transitions,
            RestaurantClient restaurantClient,
            CourierClient courierClient,
            PipelineEventPublisher eventPublisher,
            TaskScheduler taskScheduler,
            @Value("${ordermgmt.pipeline.delivery-delay.min-ms:2000}") long minDeliveryDelayMs,
            @Value("${ordermgmt.pipeline.delivery-delay.max-ms:6000}") long maxDeliveryDelayMs,
            @Value("${ordermgmt.pipeline.reconciliation.stale-after-ms:15000}") long staleAfterMs) {
        this.transitions = transitions;
        this.restaurantClient = restaurantClient;
        this.courierClient = courierClient;
        this.eventPublisher = eventPublisher;
        this.taskScheduler = taskScheduler;
        this.minDeliveryDelayMs = minDeliveryDelayMs;
        this.maxDeliveryDelayMs = maxDeliveryDelayMs;
        this.staleAfterMs = staleAfterMs;
    }

    @Override
    public void handlePlaced(OrderPlacedEvent event) {
        String orderId = event.orderId();

        if (!transitions.createPlaced(orderId, event.idempotencyKey(), event.customerId())) {
            // Not a fresh order — could be a true duplicate redelivery of
            // an order that already reached a terminal state (driveForward
            // below is then a no-op), or one that crashed mid-flight last
            // time and is still sitting in a non-terminal state with no
            // other trigger left to move it forward (see driveForward's
            // javadoc). Resume it from wherever it actually is instead of
            // silently dropping it.
            transitions.currentState(orderId).ifPresent(state -> driveForward(orderId, state));
            return;
        }
        publish(orderId, null, PLACED);
        driveForward(orderId, PLACED);
    }

    /**
     * Runs periodically to catch orders that neither Kafka redelivery nor
     * the in-memory delivery scheduler will ever revisit on their own —
     * specifically: (a) an order that crashed between two synchronous
     * transitions inside {@link #driveForward}, whose Kafka offset never
     * committed but whose {@code OrderPlacedEvent} redelivery lands on an
     * already-terminal-for-that-handler check and stops (fixed above by
     * resuming there too — this sweep is the backstop for cases where no
     * redelivery happens to occur at all), and (b) an order that reached
     * {@code OUT_FOR_DELIVERY} and had its completion scheduled purely in
     * JVM heap via {@link #scheduleDeliveryCompletion} — if the process
     * dies before that in-memory timer fires, the scheduled task is gone
     * with no trace and nothing will ever complete that order.
     */
    @Scheduled(fixedDelayString = "${ordermgmt.pipeline.reconciliation.interval-ms:10000}")
    void reconcileStaleOrders() {
        Instant threshold = Instant.now().minusMillis(staleAfterMs);
        for (StaleOrder stale : transitions.findStaleNonTerminal(threshold)) {
            log.info("orderId={} stale in state={} for >{}ms, resuming (likely recovering from an earlier outage)",
                    stale.orderId(), stale.state(), staleAfterMs);
            driveForward(stale.orderId(), stale.state());
        }
    }

    /**
     * Continues an order through its remaining lifecycle stages, entering
     * at {@code currentState}. Resumable by design — called for a freshly
     * placed order (entering at {@code PLACED}), for a redelivered
     * duplicate {@code OrderPlacedEvent} that turned out to still be
     * mid-flight, and for the reconciliation sweep above. Every step is
     * already idempotent via {@code applyTransition}'s optimistic-lock
     * guard, so re-entering at any point — including one already handled
     * by another thread in the meantime — is safe.
     */
    private void driveForward(String orderId, OrderState currentState) {
        switch (currentState) {
            case PLACED -> {
                if (advance(orderId, PLACED, CONFIRMED)) {
                    driveForward(orderId, CONFIRMED);
                }
            }
            case CONFIRMED -> {
                if (advance(orderId, CONFIRMED, PREPARING)) {
                    driveForward(orderId, PREPARING);
                }
            }
            case PREPARING -> {
                try {
                    restaurantClient.prepareOrder(orderId);
                } catch (DownstreamUnavailableException e) {
                    fail(orderId, PREPARING, "restaurant-unavailable: " + e.getMessage());
                    return;
                }
                if (advance(orderId, PREPARING, READY)) {
                    driveForward(orderId, READY);
                }
            }
            case READY -> {
                try {
                    courierClient.dispatchOrder(orderId);
                } catch (DownstreamUnavailableException e) {
                    fail(orderId, READY, "courier-unavailable: " + e.getMessage());
                    return;
                }
                if (advance(orderId, READY, OUT_FOR_DELIVERY)) {
                    driveForward(orderId, OUT_FOR_DELIVERY);
                }
            }
            case OUT_FOR_DELIVERY -> scheduleDeliveryCompletion(orderId);
            default -> {
                // DELIVERED / CANCELLED / FAILED — already terminal, nothing to drive.
            }
        }
    }

    @Override
    public void handleCancelRequested(OrderCancelRequestedEvent event) {
        String orderId = event.orderId();
        Optional<OrderState> maybeState = transitions.currentState(orderId);
        if (maybeState.isEmpty()) {
            log.warn("orderId={} cancel requested but order is unknown, ignoring", orderId);
            return;
        }

        OrderState current = maybeState.get();
        if (!OrderStateMachine.canTransition(current, CANCELLED)) {
            log.info("orderId={} cannot cancel from state={}, ignoring", orderId, current);
            return;
        }

        advance(orderId, current, CANCELLED);
    }

    private boolean advance(String orderId, OrderState from, OrderState to) {
        if (!transitions.applyTransition(orderId, from, to)) {
            return false;
        }
        publish(orderId, from, to);
        return true;
    }

    private void fail(String orderId, OrderState from, String reason) {
        if (!transitions.applyTransition(orderId, from, FAILED)) {
            return;
        }
        publish(orderId, from, FAILED);
        eventPublisher.publishFailed(new OrderFailedEvent(orderId, from, reason, Instant.now()));
    }

    private void scheduleDeliveryCompletion(String orderId) {
        long delay = minDeliveryDelayMs >= maxDeliveryDelayMs
                ? minDeliveryDelayMs
                : ThreadLocalRandom.current().nextLong(minDeliveryDelayMs, maxDeliveryDelayMs + 1);
        taskScheduler.schedule(
                () -> advance(orderId, OUT_FOR_DELIVERY, DELIVERED),
                Instant.now().plusMillis(delay));
    }

    private void publish(String orderId, OrderState from, OrderState to) {
        eventPublisher.publishStateChanged(new OrderStateChangedEvent(orderId, from, to, Instant.now(), Map.of()));
    }
}
