package com.ordermgmt.loadgen.api;

public record LoadStatusResponse(
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
