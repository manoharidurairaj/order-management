package com.ordermgmt.simulators;

import com.ordermgmt.simulators.api.SimulatorResponse;
import com.ordermgmt.simulators.chaos.ChaosDecision;
import com.ordermgmt.simulators.chaos.ChaosEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Shared by both the restaurant and courier controllers.
 */
@Service
public class ChaosSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(ChaosSimulatorService.class);

    private final ChaosEngine chaosEngine;

    public ChaosSimulatorService(ChaosEngine chaosEngine) {
        this.chaosEngine = chaosEngine;
    }

    public ResponseEntity<SimulatorResponse> simulate(String operation, String orderId) {
        ChaosDecision decision = chaosEngine.decide();
        return switch (decision.outcome()) {
            case OK -> respond(operation, orderId, HttpStatus.OK, "OK");
            case LATENCY_THEN_OK -> {
                log.info("orderId={} operation={} simulating {}ms latency", orderId, operation, decision.latencyMillis());
                sleep(decision.latencyMillis());
                yield respond(operation, orderId, HttpStatus.OK, "OK");
            }
            case SERVICE_UNAVAILABLE -> {
                log.warn("orderId={} operation={} simulating 503", orderId, operation);
                yield respond(operation, orderId, HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE");
            }
            case TOO_MANY_REQUESTS -> {
                log.warn("orderId={} operation={} simulating 429", orderId, operation);
                yield respond(operation, orderId, HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS");
            }
        };
    }

    private ResponseEntity<SimulatorResponse> respond(String operation, String orderId, HttpStatus status, String label) {
        return ResponseEntity.status(status).body(new SimulatorResponse(orderId, operation, label));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
