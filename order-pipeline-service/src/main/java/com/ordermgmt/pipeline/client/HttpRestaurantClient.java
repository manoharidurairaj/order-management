package com.ordermgmt.pipeline.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpRestaurantClient implements RestaurantClient {

    private static final Logger log = LoggerFactory.getLogger(HttpRestaurantClient.class);
    private static final String INSTANCE = "restaurant";

    private final RestClient restClient;

    public HttpRestaurantClient(RestClient simulatorsRestClient) {
        this.restClient = simulatorsRestClient;
    }

    @Override
    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "fallback")
    @Bulkhead(name = INSTANCE)
    public void prepareOrder(String orderId) {
        restClient.post()
                .uri("/restaurant/prepare")
                .body(new FulfillmentApiRequest(orderId))
                .retrieve()
                .toBodilessEntity();
    }

    // Signature must mirror the guarded method plus a trailing Throwable —
    // Resilience4j reflectively matches on this. Normalizes both retry
    // exhaustion and CallNotPermittedException (open circuit) to one type.
    private void fallback(String orderId, Throwable t) {
        log.warn("orderId={} restaurant call failed permanently: {}", orderId, t.toString());
        throw new DownstreamUnavailableException(INSTANCE, orderId, t);
    }
}
