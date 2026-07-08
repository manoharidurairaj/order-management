package com.ordermgmt.loadgen.generator;

import java.math.BigDecimal;
import java.util.List;

public record GeneratedOrder(String customerId, List<Item> items) {

    public record Item(String productId, String name, int quantity, BigDecimal unitPrice) {
    }
}
