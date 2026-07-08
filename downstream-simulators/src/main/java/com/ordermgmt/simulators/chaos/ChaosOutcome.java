package com.ordermgmt.simulators.chaos;

public enum ChaosOutcome {
    OK,
    LATENCY_THEN_OK,
    SERVICE_UNAVAILABLE,
    TOO_MANY_REQUESTS
}
