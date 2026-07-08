package com.ordermgmt.ingestion.service;

public class DuplicateOrderException extends RuntimeException {

    public DuplicateOrderException(String idempotencyKey) {
        super("Order already submitted for idempotency key: " + idempotencyKey);
    }
}
