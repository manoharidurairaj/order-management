package com.ordermgmt.ingestion.api;

public record PlaceOrderResponse(
        String orderId,
        String status
) {
}
