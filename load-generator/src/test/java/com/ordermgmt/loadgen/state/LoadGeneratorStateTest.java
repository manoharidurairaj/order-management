package com.ordermgmt.loadgen.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoadGeneratorStateTest {

    @Test
    void effectiveRateEqualsBaselineWithNoBurst() {
        LoadGeneratorState state = new LoadGeneratorState(2.0);
        assertEquals(2.0, state.effectiveRate());
        assertFalse(state.isBursting());
    }

    @Test
    void burstMultipliesBaselineWhileActive() {
        LoadGeneratorState state = new LoadGeneratorState(2.0);
        state.startBurst(5.0, 60);

        assertTrue(state.isBursting());
        assertEquals(10.0, state.effectiveRate());
    }

    @Test
    void burstExpiresAndFallsBackToBaseline() {
        LoadGeneratorState state = new LoadGeneratorState(2.0);
        state.startBurst(5.0, 0);

        // durationSeconds=0 means "until" is effectively now/past already.
        assertFalse(state.isBursting());
        assertEquals(2.0, state.effectiveRate());
    }

    @Test
    void negativeRateIsClampedToZero() {
        LoadGeneratorState state = new LoadGeneratorState(2.0);
        state.setBaselineRate(-5.0);
        assertEquals(0.0, state.baselineRate());
    }

    @Test
    void clearBurstReturnsToBaselineImmediately() {
        LoadGeneratorState state = new LoadGeneratorState(3.0);
        state.startBurst(4.0, 60);
        assertTrue(state.isBursting());

        state.clearBurst();

        assertFalse(state.isBursting());
        assertEquals(3.0, state.effectiveRate());
    }

    @Test
    void noMaxOrdersMeansRequestedBudgetIsUnchanged() {
        LoadGeneratorState state = new LoadGeneratorState(2.0);
        assertEquals(5, state.reserveEmitBudget(5));
        assertEquals(0, state.ordersSubmitted());
    }

    @Test
    void reserveEmitBudgetClampsToWhatsLeftAndTracksTheCount() {
        LoadGeneratorState state = new LoadGeneratorState(2.0);
        state.setMaxOrders(10L);

        assertEquals(7, state.reserveEmitBudget(7));
        assertEquals(7, state.ordersSubmitted());

        // Only 3 left of the budget of 10, even though 7 were requested again.
        assertEquals(3, state.reserveEmitBudget(7));
        assertEquals(10, state.ordersSubmitted());
    }

    @Test
    void reachingMaxOrdersAutoStopsTheBaselineRate() {
        LoadGeneratorState state = new LoadGeneratorState(2.0);
        state.setMaxOrders(5L);

        assertEquals(5, state.reserveEmitBudget(5));
        assertEquals(0.0, state.baselineRate());
        assertEquals(0, state.reserveEmitBudget(1));
    }

    @Test
    void settingMaxOrdersAgainResetsTheSubmittedCount() {
        LoadGeneratorState state = new LoadGeneratorState(2.0);
        state.setMaxOrders(5L);
        state.reserveEmitBudget(5);
        assertEquals(5, state.ordersSubmitted());

        state.setMaxOrders(10L);

        assertEquals(0, state.ordersSubmitted());
        assertEquals(10L, state.maxOrders());
    }
}
