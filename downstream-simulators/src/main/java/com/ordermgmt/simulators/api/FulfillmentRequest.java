package com.ordermgmt.simulators.api;

import jakarta.validation.constraints.NotBlank;

public record FulfillmentRequest(@NotBlank String orderId) {
}
