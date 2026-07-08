package com.ordermgmt.common.domain;

import java.math.BigDecimal;

public record OrderItem(
        String productId,
        String name,
        int quantity,
        BigDecimal unitPrice
) {
}
