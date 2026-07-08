package com.ordermgmt.simulators.chaos;

public record ChaosDecision(ChaosOutcome outcome, long latencyMillis) {

    public static ChaosDecision immediate(ChaosOutcome outcome) {
        return new ChaosDecision(outcome, 0L);
    }
}
