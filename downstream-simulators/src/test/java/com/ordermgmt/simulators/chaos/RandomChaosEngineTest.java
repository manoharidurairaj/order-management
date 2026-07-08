package com.ordermgmt.simulators.chaos;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomChaosEngineTest {

    @Test
    void distributionRoughlyMatchesConfiguredProbabilitiesOverManyTrials() {
        ChaosProperties properties = new ChaosProperties(
                new ChaosProperties.Latency(0.20, 3000, 5000),
                0.15,
                0.10);
        RandomChaosEngine engine = new RandomChaosEngine(new ChaosState(properties));

        int trials = 100_000;
        Map<ChaosOutcome, Integer> counts = new EnumMap<>(ChaosOutcome.class);
        for (ChaosOutcome outcome : ChaosOutcome.values()) {
            counts.put(outcome, 0);
        }
        for (int i = 0; i < trials; i++) {
            ChaosOutcome outcome = engine.decide().outcome();
            counts.merge(outcome, 1, Integer::sum);
        }

        assertWithinTolerance(counts.get(ChaosOutcome.LATENCY_THEN_OK), trials, 0.20);
        assertWithinTolerance(counts.get(ChaosOutcome.SERVICE_UNAVAILABLE), trials, 0.15);
        assertWithinTolerance(counts.get(ChaosOutcome.TOO_MANY_REQUESTS), trials, 0.10);
        assertWithinTolerance(counts.get(ChaosOutcome.OK), trials, 0.55);
    }

    @Test
    void latencyOutcomeReportsLatencyWithinConfiguredBounds() {
        ChaosProperties properties = new ChaosProperties(
                new ChaosProperties.Latency(1.0, 3000, 5000),
                0.0,
                0.0);
        RandomChaosEngine engine = new RandomChaosEngine(new ChaosState(properties));

        for (int i = 0; i < 1000; i++) {
            ChaosDecision decision = engine.decide();
            assertEquals(ChaosOutcome.LATENCY_THEN_OK, decision.outcome());
            assertTrue(decision.latencyMillis() >= 3000 && decision.latencyMillis() <= 5000);
        }
    }

    @Test
    void zeroProbabilityAlwaysYieldsOk() {
        ChaosProperties properties = new ChaosProperties(
                new ChaosProperties.Latency(0.0, 3000, 5000),
                0.0,
                0.0);
        RandomChaosEngine engine = new RandomChaosEngine(new ChaosState(properties));

        for (int i = 0; i < 1000; i++) {
            assertEquals(ChaosOutcome.OK, engine.decide().outcome());
        }
    }

    private void assertWithinTolerance(int actualCount, int trials, double expectedRate) {
        double actualRate = (double) actualCount / trials;
        assertTrue(Math.abs(actualRate - expectedRate) < 0.02,
                "expected rate ~%.2f but got %.4f".formatted(expectedRate, actualRate));
    }
}
