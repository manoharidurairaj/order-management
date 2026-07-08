package com.ordermgmt.ingestion.service;

import com.ordermgmt.common.domain.OrderCancelRequestedEvent;
import com.ordermgmt.common.domain.OrderItem;
import com.ordermgmt.common.domain.OrderPlacedEvent;
import com.ordermgmt.ingestion.api.OrderItemRequest;
import com.ordermgmt.ingestion.api.PlaceOrderRequest;
import com.ordermgmt.ingestion.idempotency.IdempotencyGuard;
import com.ordermgmt.ingestion.kafka.OrderEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 *  Order Ingestion service: Save to Redis and publish to Kafka.
 */
@Service
public class DefaultOrderIngestionService implements OrderIngestionService {

    private final IdempotencyGuard idempotencyGuard;
    private final OrderEventPublisher eventPublisher;

    public DefaultOrderIngestionService(IdempotencyGuard idempotencyGuard, OrderEventPublisher eventPublisher) {
        this.idempotencyGuard = idempotencyGuard;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String placeOrder(PlaceOrderRequest request, String idempotencyKey) {
        if (!idempotencyGuard.tryAcquire(idempotencyKey)) {
            throw new DuplicateOrderException(idempotencyKey);
        }

        String orderId = UUID.randomUUID().toString();
        List<OrderItem> items = request.items().stream()
                .map(this::toDomainItem)
                .toList();

        OrderPlacedEvent event = new OrderPlacedEvent(
                orderId,
                idempotencyKey,
                request.customerId(),
                items,
                Instant.now());

        eventPublisher.publishOrderPlaced(event);
        return orderId;
    }

    @Override
    public void requestCancellation(String orderId) {
        eventPublisher.publishCancelRequested(new OrderCancelRequestedEvent(orderId, Instant.now()));
    }

    private OrderItem toDomainItem(OrderItemRequest item) {
        return new OrderItem(item.productId(), item.name(), item.quantity(), item.unitPrice());
    }
}
