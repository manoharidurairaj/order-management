package com.ordermgmt.pipeline.orchestration;

import com.ordermgmt.common.domain.*;
import com.ordermgmt.pipeline.client.CourierClient;
import com.ordermgmt.pipeline.client.DownstreamUnavailableException;
import com.ordermgmt.pipeline.client.RestaurantClient;
import com.ordermgmt.pipeline.kafka.PipelineEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.ordermgmt.common.domain.OrderState.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DefaultOrderOrchestratorTest {

    private OrderTransitionService transitions;
    private RestaurantClient restaurantClient;
    private CourierClient courierClient;
    private PipelineEventPublisher eventPublisher;
    private TaskScheduler taskScheduler;
    private DefaultOrderOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        transitions = mock(OrderTransitionService.class);
        restaurantClient = mock(RestaurantClient.class);
        courierClient = mock(CourierClient.class);
        eventPublisher = mock(PipelineEventPublisher.class);
        taskScheduler = mock(TaskScheduler.class);
        orchestrator = new DefaultOrderOrchestrator(
                transitions, restaurantClient, courierClient, eventPublisher, taskScheduler, 10, 20);

        // Every transition succeeds by default; individual tests override.
        when(transitions.createPlaced(any(), any(), any())).thenReturn(true);
        when(transitions.applyTransition(any(), any(), any())).thenReturn(true);
    }

    private OrderPlacedEvent placedEvent(String orderId) {
        return new OrderPlacedEvent(orderId, "idem-" + orderId, "customer-1", List.of(), Instant.now());
    }

    @Test
    void happyPathRunsThroughToScheduledDelivery() {
        orchestrator.handlePlaced(placedEvent("order-1"));

        verify(transitions).createPlaced("order-1", "idem-order-1", "customer-1");
        verify(transitions).applyTransition("order-1", PLACED, CONFIRMED);
        verify(transitions).applyTransition("order-1", CONFIRMED, PREPARING);
        verify(restaurantClient).prepareOrder("order-1");
        verify(transitions).applyTransition("order-1", PREPARING, READY);
        verify(courierClient).dispatchOrder("order-1");
        verify(transitions).applyTransition("order-1", READY, OUT_FOR_DELIVERY);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(captor.capture(), any(Instant.class));

        // Simulate the scheduled delivery-completion task firing.
        captor.getValue().run();
        verify(transitions).applyTransition("order-1", OUT_FOR_DELIVERY, DELIVERED);
    }

    @Test
    void duplicatePlacedEventIsANoOpAfterCreateFails() {
        when(transitions.createPlaced(any(), any(), any())).thenReturn(false);

        orchestrator.handlePlaced(placedEvent("order-1"));

        verifyNoInteractions(restaurantClient, courierClient, taskScheduler);
        verify(transitions, never()).applyTransition(any(), any(), any());
    }

    @Test
    void restaurantFailureRoutesToFailedAndDlqWithoutCallingCourier() {
        doThrow(new DownstreamUnavailableException("restaurant", "order-1", new RuntimeException("boom")))
                .when(restaurantClient).prepareOrder("order-1");

        orchestrator.handlePlaced(placedEvent("order-1"));

        verify(transitions).applyTransition("order-1", PREPARING, FAILED);
        verify(eventPublisher).publishFailed(argThat(e ->
                e.orderId().equals("order-1") && e.failedAtState() == PREPARING));
        verifyNoInteractions(courierClient);
    }

    @Test
    void courierFailureRoutesToFailedAndDlq() {
        doThrow(new DownstreamUnavailableException("courier", "order-1", new RuntimeException("boom")))
                .when(courierClient).dispatchOrder("order-1");

        orchestrator.handlePlaced(placedEvent("order-1"));

        verify(transitions).applyTransition("order-1", READY, FAILED);
        verify(eventPublisher).publishFailed(argThat(e ->
                e.orderId().equals("order-1") && e.failedAtState() == READY));
        verifyNoInteractions(taskScheduler);
    }

    @Test
    void staleTransitionStopsTheSequenceWithoutError() {
        when(transitions.applyTransition("order-1", PLACED, CONFIRMED)).thenReturn(false);

        orchestrator.handlePlaced(placedEvent("order-1"));

        verify(transitions, never()).applyTransition("order-1", CONFIRMED, PREPARING);
        verifyNoInteractions(restaurantClient, courierClient);
    }

    @Test
    void cancelRequestedAppliesTransitionWhenLegal() {
        when(transitions.currentState("order-1")).thenReturn(Optional.of(CONFIRMED));

        orchestrator.handleCancelRequested(new OrderCancelRequestedEvent("order-1", Instant.now()));

        verify(transitions).applyTransition("order-1", CONFIRMED, CANCELLED);
    }

    @Test
    void cancelRequestedIgnoredWhenOrderAlreadyPreparing() {
        when(transitions.currentState("order-1")).thenReturn(Optional.of(PREPARING));

        orchestrator.handleCancelRequested(new OrderCancelRequestedEvent("order-1", Instant.now()));

        verify(transitions, never()).applyTransition(eq("order-1"), any(), eq(CANCELLED));
    }

    @Test
    void cancelRequestedIgnoredWhenOrderUnknown() {
        when(transitions.currentState("ghost")).thenReturn(Optional.empty());

        orchestrator.handleCancelRequested(new OrderCancelRequestedEvent("ghost", Instant.now()));

        verifyNoInteractions(eventPublisher);
    }
}
