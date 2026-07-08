package com.ordermgmt.ingestion.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Order line item.
 */
public record OrderItemRequest(
        @NotBlank String productId,
        @NotBlank String name,
        @Min(1) int quantity,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal unitPrice
) {
}
