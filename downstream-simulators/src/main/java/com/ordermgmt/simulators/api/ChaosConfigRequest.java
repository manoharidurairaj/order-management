package com.ordermgmt.simulators.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record ChaosConfigRequest(
        @DecimalMin("0.0") @DecimalMax("1.0") double latencyProbability,
        @DecimalMin("0.0") @DecimalMax("1.0") double rate503,
        @DecimalMin("0.0") @DecimalMax("1.0") double rate429
) {
}
