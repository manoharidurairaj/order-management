package com.ordermgmt.pipeline.client;

public interface CourierClient {

    /**
     * @throws DownstreamUnavailableException if the retry budget was
     * exhausted or the circuit breaker is open.
     */
    void dispatchOrder(String orderId);
}
