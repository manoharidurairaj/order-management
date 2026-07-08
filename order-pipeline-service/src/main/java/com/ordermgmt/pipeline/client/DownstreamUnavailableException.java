package com.ordermgmt.pipeline.client;

/**
 * Exception thrown for Downstream unavailable cases.
 */
public class DownstreamUnavailableException extends RuntimeException {

    public DownstreamUnavailableException(String downstream, String orderId, Throwable cause) {
        super("Downstream '%s' unavailable for orderId=%s".formatted(downstream, orderId), cause);
    }
}
