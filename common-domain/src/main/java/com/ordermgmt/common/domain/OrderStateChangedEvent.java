package com.ordermgmt.common.domain;

import java.time.Instant;
import java.util.Map;

/**
 * One generic event type for every transition. Consumers
 * key off {@code toState}; adding a new stage never requires a new event
 * class or a new deserializer registration.
 */
public record OrderStateChangedEvent(
        String orderId,
        OrderState fromState,
        OrderState toState,
        Instant occurredAt,
        Map<String, String> metadata
) {
}
