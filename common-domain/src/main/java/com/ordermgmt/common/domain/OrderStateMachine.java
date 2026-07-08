package com.ordermgmt.common.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.ordermgmt.common.domain.OrderState.*;

/**
 * The single source of truth for legal order-state transitions.
 */
public final class OrderStateMachine {

    private static final Map<OrderState, Set<OrderState>> TRANSITIONS = new EnumMap<>(OrderState.class);

    static {
        TRANSITIONS.put(PLACED, EnumSet.of(CONFIRMED, CANCELLED));
        TRANSITIONS.put(CONFIRMED, EnumSet.of(PREPARING, CANCELLED));
        TRANSITIONS.put(PREPARING, EnumSet.of(READY, FAILED));
        TRANSITIONS.put(READY, EnumSet.of(OUT_FOR_DELIVERY, FAILED));
        TRANSITIONS.put(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED, FAILED));
        TRANSITIONS.put(DELIVERED, EnumSet.noneOf(OrderState.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderState.class));
        TRANSITIONS.put(FAILED, EnumSet.noneOf(OrderState.class));
    }

    private OrderStateMachine() {
    }

    public static boolean canTransition(OrderState from, OrderState to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static boolean isTerminal(OrderState state) {
        return TRANSITIONS.getOrDefault(state, Set.of()).isEmpty();
    }

    public static Set<OrderState> validNextStates(OrderState from) {
        return Set.copyOf(TRANSITIONS.getOrDefault(from, Set.of()));
    }
}
