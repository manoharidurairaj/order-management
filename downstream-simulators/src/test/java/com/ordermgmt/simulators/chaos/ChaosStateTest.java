package com.ordermgmt.simulators.chaos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChaosStateTest {

    @Test
    void currentReflectsTheSeededProperties() {
        ChaosProperties seed = new ChaosProperties(new ChaosProperties.Latency(0.20, 3000, 5000), 0.15, 0.10);
        ChaosState state = new ChaosState(seed);

        ChaosProperties current = state.current();
        assertEquals(0.20, current.latency().probability());
        assertEquals(3000, current.latency().minMs());
        assertEquals(5000, current.latency().maxMs());
        assertEquals(0.15, current.rate503());
        assertEquals(0.10, current.rate429());
    }

    @Test
    void updateChangesTheRatesButLeavesLatencyBoundsFixed() {
        ChaosProperties seed = new ChaosProperties(new ChaosProperties.Latency(0.20, 3000, 5000), 0.15, 0.10);
        ChaosState state = new ChaosState(seed);

        state.update(0.5, 0.3, 0.1);

        ChaosProperties current = state.current();
        assertEquals(0.5, current.latency().probability());
        assertEquals(0.3, current.rate503());
        assertEquals(0.1, current.rate429());
        // latency bounds are fixed at startup, not tunable via update()
        assertEquals(3000, current.latency().minMs());
        assertEquals(5000, current.latency().maxMs());
    }
}
