package com.ordermgmt.common.domain;

/**
 * The full lifecycle of an order.
 */
public enum OrderState {
    PLACED,
    CONFIRMED,
    PREPARING,
    READY,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    FAILED
}
