package com.ordermgmt.common.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.ordermgmt.common.domain.OrderState.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStateMachineTest {

    @Test
    void happyPathIsFullyLegal() {
        assertTrue(OrderStateMachine.canTransition(PLACED, CONFIRMED));
        assertTrue(OrderStateMachine.canTransition(CONFIRMED, PREPARING));
        assertTrue(OrderStateMachine.canTransition(PREPARING, READY));
        assertTrue(OrderStateMachine.canTransition(READY, OUT_FOR_DELIVERY));
        assertTrue(OrderStateMachine.canTransition(OUT_FOR_DELIVERY, DELIVERED));
    }

    @Test
    void cancellationOnlyAllowedBeforePreparationStarts() {
        assertTrue(OrderStateMachine.canTransition(PLACED, CANCELLED));
        assertTrue(OrderStateMachine.canTransition(CONFIRMED, CANCELLED));
        assertFalse(OrderStateMachine.canTransition(PREPARING, CANCELLED));
        assertFalse(OrderStateMachine.canTransition(READY, CANCELLED));
    }

    @Test
    void failureOnlyAllowedOnceFulfillmentHasStarted() {
        assertTrue(OrderStateMachine.canTransition(PREPARING, FAILED));
        assertTrue(OrderStateMachine.canTransition(READY, FAILED));
        assertTrue(OrderStateMachine.canTransition(OUT_FOR_DELIVERY, FAILED));
        assertFalse(OrderStateMachine.canTransition(PLACED, FAILED));
    }

    @Test
    void cannotSkipStages() {
        assertFalse(OrderStateMachine.canTransition(PLACED, PREPARING));
        assertFalse(OrderStateMachine.canTransition(PLACED, OUT_FOR_DELIVERY));
        assertFalse(OrderStateMachine.canTransition(CONFIRMED, OUT_FOR_DELIVERY));
        assertFalse(OrderStateMachine.canTransition(PREPARING, DELIVERED));
    }

    @Test
    void cannotGoBackwards() {
        assertFalse(OrderStateMachine.canTransition(PREPARING, CONFIRMED));
        assertFalse(OrderStateMachine.canTransition(READY, PREPARING));
        assertFalse(OrderStateMachine.canTransition(DELIVERED, OUT_FOR_DELIVERY));
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"DELIVERED", "CANCELLED", "FAILED"})
    void terminalStatesHaveNoOutgoingTransitions(OrderState terminal) {
        assertTrue(OrderStateMachine.isTerminal(terminal));
        assertTrue(OrderStateMachine.validNextStates(terminal).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"PLACED", "CONFIRMED", "PREPARING", "READY", "OUT_FOR_DELIVERY"})
    void nonTerminalStatesHaveAtLeastOneOutgoingTransition(OrderState nonTerminal) {
        assertFalse(OrderStateMachine.isTerminal(nonTerminal));
        assertFalse(OrderStateMachine.validNextStates(nonTerminal).isEmpty());
    }
}
