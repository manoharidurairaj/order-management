package com.ordermgmt.simulators.chaos;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Failure/latency profile is config-driven, not hardcoded, so the same lever
 * used to dial load up/down (the load generator) can be matched with a
 * chaos profile dialed up/down for the demo — no redeploy needed.
 */
@ConfigurationProperties(prefix = "chaos")
public record ChaosProperties(
        Latency latency,
        double rate503,
        double rate429
) {
    public record Latency(double probability, long minMs, long maxMs) {
    }
}
