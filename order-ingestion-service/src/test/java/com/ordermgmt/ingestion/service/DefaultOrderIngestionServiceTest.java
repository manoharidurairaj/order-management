package com.ordermgmt.ingestion.service;

import com.ordermgmt.common.domain.OrderCancelRequestedEvent;
import com.ordermgmt.common.domain.OrderPlacedEvent;
import com.ordermgmt.ingestion.api.OrderItemRequest;
import com.ordermgmt.ingestion.api.PlaceOrderRequest;
import com.ordermgmt.ingestion.idempotency.IdempotencyGuard;
import com.ordermgmt.ingestion.kafka.OrderEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultOrderIngestionServiceTest {

    private IdempotencyGuard idempotencyGuard;
    private OrderEventPublisher eventPublisher;
    private DefaultOrderIngestionService service;

    @BeforeEach
    void setUp() {
        idempotencyGuard = mock(IdempotencyGuard.class);
        eventPublisher = mock(OrderEventPublisher.class);
        service = new DefaultOrderIngestionService(idempotencyGuard, eventPublisher);
    }

    @Test
    void publishesOrderPlacedEventWhenKeyIsFresh() {
        when(idempotencyGuard.tryAcquire("key-1")).thenReturn(true);
        PlaceOrderRequest request = new PlaceOrderRequest(
                "customer-1",
                List.of(new OrderItemRequest("sku-1", "Pizza", 2, BigDecimal.valueOf(9.99))));

        String orderId = service.placeOrder(request, "key-1");

        assertNotNull(orderId);
        ArgumentCaptor<OrderPlacedEvent> captor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(eventPublisher).publishOrderPlaced(captor.capture());
        OrderPlacedEvent published = captor.getValue();
        assertEquals(orderId, published.orderId());
        assertEquals("key-1", published.idempotencyKey());
        assertEquals("customer-1", published.customerId());
        assertEquals(1, published.items().size());
    }

    @Test
    void rejectsDuplicateIdempotencyKeyWithoutPublishing() {
        when(idempotencyGuard.tryAcquire("key-1")).thenReturn(false);
        PlaceOrderRequest request = new PlaceOrderRequest(
                "customer-1",
                List.of(new OrderItemRequest("sku-1", "Pizza", 1, BigDecimal.valueOf(9.99))));

        assertThrows(DuplicateOrderException.class, () -> service.placeOrder(request, "key-1"));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void requestCancellationPublishesCancelRequestedEvent() {
        service.requestCancellation("order-42");

        ArgumentCaptor<OrderCancelRequestedEvent> captor = ArgumentCaptor.forClass(OrderCancelRequestedEvent.class);
        verify(eventPublisher).publishCancelRequested(captor.capture());
        assertEquals("order-42", captor.getValue().orderId());
    }
}
