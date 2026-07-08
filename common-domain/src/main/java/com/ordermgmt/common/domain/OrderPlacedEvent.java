package com.ordermgmt.common.domain;

import java.time.Instant;
import java.util.List;

/**
 * Emitted exactly once, by the ingestion service, per accepted order.
 * Carries the idempotency key alongside the order id purely for audit /
 * traceability — dedup itself already happened in Redis before this event
 * was ever published.
 */
public record OrderPlacedEvent(
        String orderId,
        String idempotencyKey,
        String customerId,
        List<OrderItem> items,
        Instant occurredAt
) {
}
