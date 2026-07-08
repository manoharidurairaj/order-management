package com.ordermgmt.pipeline.orchestration;

import com.ordermgmt.common.domain.OrderState;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The transactional core that makes redelivery/duplicate Kafka messages
 * safe.
 */
public interface OrderTransitionService {

    /**
     * Inserts a new order in {@code PLACED} state.
     *
     * @return true if inserted, false if an order with this id already
     * exists (duplicate {@code OrderPlacedEvent} redelivery).
     */
    boolean createPlaced(String orderId, String idempotencyKey, String customerId);

    Optional<OrderState> currentState(String orderId);

    /**
     * Applies {@code from -> to} only if the order is currently in state
     * {@code from} and the transition is legal per {@link
     * com.ordermgmt.common.domain.OrderStateMachine}. Returns false (a
     * no-op, not an error) for stale/duplicate/illegal transitions — this
     * is the idempotency boundary against Kafka redelivery.
     */
    boolean applyTransition(String orderId, OrderState from, OrderState to);

    /**
     * Orders in a non-terminal state that haven't been updated since
     * before {@code olderThan} — candidates for the reconciliation sweep
     * to resume, since neither Kafka redelivery nor the in-memory delivery
     * scheduler is guaranteed to ever revisit them after a crash.
     */
    List<StaleOrder> findStaleNonTerminal(Instant olderThan);
}
