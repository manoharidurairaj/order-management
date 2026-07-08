package com.ordermgmt.simulators.api;

import com.ordermgmt.simulators.ChaosSimulatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CourierController {

    private final ChaosSimulatorService chaosSimulatorService;

    public CourierController(ChaosSimulatorService chaosSimulatorService) {
        this.chaosSimulatorService = chaosSimulatorService;
    }

    @PostMapping("/courier/dispatch")
    public ResponseEntity<SimulatorResponse> dispatch(@Valid @RequestBody FulfillmentRequest request) {
        return chaosSimulatorService.simulate("DISPATCH", request.orderId());
    }
}
