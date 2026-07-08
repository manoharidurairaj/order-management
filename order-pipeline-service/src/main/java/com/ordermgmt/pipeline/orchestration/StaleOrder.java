package com.ordermgmt.pipeline.orchestration;

import com.ordermgmt.common.domain.OrderState;

/**
 * An order sitting in a non-terminal state for longer than expected —
 * either mid-flight when the process crashed (its Kafka message's offset
 * never committed, but a later message that already reached a further
 * state won't trigger redelivery to notice) or a delivery-completion task
 * that was scheduled purely in JVM memory and lost when the process died.
 * Surfaced by {@link OrderTransitionService#findStaleNonTerminal} for the
 * reconciliation sweep to resume.
 */
public record StaleOrder(String orderId, OrderState state) {
}
