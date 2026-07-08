package com.ordermgmt.dashboard.loadgen;

/**
 * Mirrors load-generator's {@code /load/status} response shape in full, so
 * deserialization doesn't depend on lenient unknown-property handling.
 */
public record LoadGeneratorStatusResponse(
        double baselineRate,
        boolean bursting,
        double effectiveRate,
        Long maxOrders,
        long ordersSubmitted,
        long ordersAccepted,
        long ordersDuplicateRejected,
        long ordersErrors
) {
}
