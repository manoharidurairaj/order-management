package com.ordermgmt.common.domain;

import java.time.Instant;

/**
 * Cancel Order event.
 */
public record OrderCancelRequestedEvent(
        String orderId,
        Instant occurredAt
) {
}
