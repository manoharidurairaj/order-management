package com.ordermgmt.pipeline.client;

/**
 * Narrow, single-method interface (ISP) the orchestrator depends on instead
 * of a concrete HTTP client (DIP) — swapping the simulator for a real
 * restaurant integration later means a new implementation of this
 * interface, not a change to orchestration logic.
 */
public interface RestaurantClient {

    /**
     * @throws DownstreamUnavailableException if the retry budget was
     * exhausted or the circuit breaker is open.
     */
    void prepareOrder(String orderId);
}
