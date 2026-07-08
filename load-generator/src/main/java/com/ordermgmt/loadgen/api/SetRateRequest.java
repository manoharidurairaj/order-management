package com.ordermgmt.loadgen.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

/**
 * {@code maxOrders} is optional — omit it (or pass {@code null}) for an
 * unbounded rate, same as before this field existed. When present, the
 * generator auto-stops itself once that many total orders have been
 * submitted at this rate.
 */
public record SetRateRequest(
        @DecimalMin("0.0") double ordersPerSecond,
        @Min(1) Long maxOrders
) {
}
