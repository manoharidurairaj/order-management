package com.ordermgmt.ingestion.service;

import com.ordermgmt.ingestion.api.PlaceOrderRequest;

public interface OrderIngestionService {

    /**
     * @return the generated order id.
     * @throws DuplicateOrderException if this idempotency key has already
     * been accepted.
     */
    String placeOrder(PlaceOrderRequest request, String idempotencyKey);

    void requestCancellation(String orderId);
}
