package com.ordermgmt.pipeline.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpCourierClient implements CourierClient {

    private static final Logger log = LoggerFactory.getLogger(HttpCourierClient.class);
    private static final String INSTANCE = "courier";

    private final RestClient restClient;

    public HttpCourierClient(RestClient simulatorsRestClient) {
        this.restClient = simulatorsRestClient;
    }

    @Override
    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "fallback")
    @Bulkhead(name = INSTANCE)
    public void dispatchOrder(String orderId) {
        restClient.post()
                .uri("/courier/dispatch")
                .body(new FulfillmentApiRequest(orderId))
                .retrieve()
                .toBodilessEntity();
    }

    private void fallback(String orderId, Throwable t) {
        log.warn("orderId={} courier call failed permanently: {}", orderId, t.toString());
        throw new DownstreamUnavailableException(INSTANCE, orderId, t);
    }
}
