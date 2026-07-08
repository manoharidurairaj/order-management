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

    public DefaultOrderOrchestrator(
            OrderTransitionService transitions,
            RestaurantClient restaurantClient,
            CourierClient courierClient,
            PipelineEventPublisher eventPublisher,
            TaskScheduler taskScheduler,
            @Value("${ordermgmt.pipeline.delivery-delay.min-ms:2000}") long minDeliveryDelayMs,
            @Value("${ordermgmt.pipeline.delivery-delay.max-ms:6000}") long maxDeliveryDelayMs) {
        this.transitions = transitions;
        this.restaurantClient = restaurantClient;
        this.courierClient = courierClient;
        this.eventPublisher = eventPublisher;
        this.taskScheduler = taskScheduler;
        this.minDeliveryDelayMs = minDeliveryDelayMs;
        this.maxDeliveryDelayMs = maxDeliveryDelayMs;
    }

    @Override
    public void handlePlaced(OrderPlacedEvent event) {
        String orderId = event.orderId();

        if (!transitions.createPlaced(orderId, event.idempotencyKey(), event.customerId())) {
            return;
        }
        publish(orderId, null, PLACED);

        if (!advance(orderId, PLACED, CONFIRMED)) return;
        if (!advance(orderId, CONFIRMED, PREPARING)) return;

        try {
            restaurantClient.prepareOrder(orderId);
        } catch (DownstreamUnavailableException e) {
            fail(orderId, PREPARING, "restaurant-unavailable: " + e.getMessage());
            return;
        }

        if (!advance(orderId, PREPARING, READY)) return;

        try {
            courierClient.dispatchOrder(orderId);
        } catch (DownstreamUnavailableException e) {
            fail(orderId, READY, "courier-unavailable: " + e.getMessage());
            return;
        }

        if (!advance(orderId, READY, OUT_FOR_DELIVERY)) return;

        scheduleDeliveryCompletion(orderId);
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
