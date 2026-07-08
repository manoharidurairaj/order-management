package com.ordermgmt.ingestion.api;

import com.ordermgmt.ingestion.service.OrderIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderIngestionService orderIngestionService;

    public OrderController(OrderIngestionService orderIngestionService) {
        this.orderIngestionService = orderIngestionService;
    }

    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request) {
        String orderId = orderIngestionService.placeOrder(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new PlaceOrderResponse(orderId, "ACCEPTED"));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId) {
        orderIngestionService.requestCancellation(orderId);
        return ResponseEntity.accepted().build();
    }
}
