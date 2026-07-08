package com.ordermgmt.loadgen.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record BurstRequest(
        @DecimalMin("1.0") double multiplier,
        @Min(1) long durationSeconds
) {
}
