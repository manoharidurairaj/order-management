package com.ordermgmt.common.domain;

import java.time.Instant;

/**
 * Order Fail event.
 */
public record OrderFailedEvent(
        String orderId,
        OrderState failedAtState,
        String reason,
        Instant occurredAt
) {
}
