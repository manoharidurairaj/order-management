package com.ordermgmt.pipeline.client;

/**
 * Dummy fulfillment request to downstream service.
 */
public record FulfillmentApiRequest(String orderId) {
}
