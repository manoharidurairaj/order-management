package com.ordermgmt.simulators.api;

import com.ordermgmt.simulators.ChaosSimulatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestaurantController {

    private final ChaosSimulatorService chaosSimulatorService;

    public RestaurantController(ChaosSimulatorService chaosSimulatorService) {
        this.chaosSimulatorService = chaosSimulatorService;
    }

    @PostMapping("/restaurant/prepare")
    public ResponseEntity<SimulatorResponse> prepare(@Valid @RequestBody FulfillmentRequest request) {
        return chaosSimulatorService.simulate("PREPARE", request.orderId());
    }
}
