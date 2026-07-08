package com.ordermgmt.pipeline.orchestration;

import com.ordermgmt.common.domain.OrderState;
import com.ordermgmt.common.domain.OrderStateMachine;
import com.ordermgmt.pipeline.domain.OrderEntity;
import com.ordermgmt.pipeline.domain.OrderHistoryEntity;
import com.ordermgmt.pipeline.domain.OrderHistoryRepository;
import com.ordermgmt.pipeline.domain.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.ordermgmt.common.domain.OrderState.*;

@Component
public class JpaOrderTransitionService implements OrderTransitionService {

    private static final Logger log = LoggerFactory.getLogger(JpaOrderTransitionService.class);

    private static final List<OrderState> NON_TERMINAL_STATES =
            List.of(PLACED, CONFIRMED, PREPARING, READY, OUT_FOR_DELIVERY);

    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;

    public JpaOrderTransitionService(OrderRepository orderRepository, OrderHistoryRepository orderHistoryRepository) {
        this.orderRepository = orderRepository;
        this.orderHistoryRepository = orderHistoryRepository;
    }

    @Override
    @Transactional
    public boolean createPlaced(String orderId, String idempotencyKey, String customerId) {
        if (orderRepository.existsById(orderId)) {
            log.info("orderId={} already exists, skipping duplicate OrderPlacedEvent", orderId);
            return false;
        }
        try {
            orderRepository.saveAndFlush(new OrderEntity(orderId, idempotencyKey, customerId, OrderState.PLACED));
        } catch (DataIntegrityViolationException e) {
            log.info("orderId={} lost the race to create the order (concurrent redelivery), skipping", orderId);
            return false;
        }
        recordHistory(orderId, null, OrderState.PLACED);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderState> currentState(String orderId) {
        return orderRepository.findById(orderId).map(OrderEntity::getState);
    }

    @Override
    @Transactional
    public boolean applyTransition(String orderId, OrderState from, OrderState to) {
        Optional<OrderEntity> maybeOrder = orderRepository.findById(orderId);
        if (maybeOrder.isEmpty()) {
            log.warn("orderId={} not found, cannot transition to {}", orderId, to);
            return false;
        }

        OrderEntity order = maybeOrder.get();
        if (order.getState() != from) {
            log.info("orderId={} expected state={} but actual={}, skipping transition to {} "
                    + "(stale or duplicate event)", orderId, from, order.getState(), to);
            return false;
        }
        if (!OrderStateMachine.canTransition(from, to)) {
            log.warn("orderId={} illegal transition {} -> {}, skipping", orderId, from, to);
            return false;
        }

        order.setState(to);
        try {
            orderRepository.saveAndFlush(order);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.info("orderId={} lost optimistic-lock race transitioning {} -> {} (concurrent redelivery), skipping",
                    orderId, from, to);
            return false;
        }

        recordHistory(orderId, from, to);
        log.info("orderId={} state {} -> {}", orderId, from, to);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaleOrder> findStaleNonTerminal(Instant olderThan) {
        return orderRepository.findByStateInAndUpdatedAtBefore(NON_TERMINAL_STATES, olderThan).stream()
                .map(order -> new StaleOrder(order.getId(), order.getState()))
                .toList();
    }

    private void recordHistory(String orderId, OrderState from, OrderState to) {
        orderHistoryRepository.save(new OrderHistoryEntity(orderId, from, to, Instant.now()));
    }
}
