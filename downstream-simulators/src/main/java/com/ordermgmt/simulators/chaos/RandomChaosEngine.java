package com.ordermgmt.simulators.chaos;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomChaosEngine implements ChaosEngine {

    private final ChaosState state;

    public RandomChaosEngine(ChaosState state) {
        this.state = state;
    }

    @Override
    public ChaosDecision decide() {
        ChaosProperties properties = state.current();
        double r = ThreadLocalRandom.current().nextDouble();

        double latencyThreshold = properties.latency().probability();
        double unavailableThreshold = latencyThreshold + properties.rate503();
        double tooManyThreshold = unavailableThreshold + properties.rate429();

        if (r < latencyThreshold) {
            long min = properties.latency().minMs();
            long max = properties.latency().maxMs();
            long latency = min >= max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
            return new ChaosDecision(ChaosOutcome.LATENCY_THEN_OK, latency);
        }
        if (r < unavailableThreshold) {
            return ChaosDecision.immediate(ChaosOutcome.SERVICE_UNAVAILABLE);
        }
        if (r < tooManyThreshold) {
            return ChaosDecision.immediate(ChaosOutcome.TOO_MANY_REQUESTS);
        }
        return ChaosDecision.immediate(ChaosOutcome.OK);
    }
}
