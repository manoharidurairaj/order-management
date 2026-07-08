package com.ordermgmt.simulators.api;

public record ChaosConfigResponse(
        double latencyProbability,
        long latencyMinMs,
        long latencyMaxMs,
        double rate503,
        double rate429
) {
}
