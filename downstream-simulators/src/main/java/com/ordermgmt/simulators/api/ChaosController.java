package com.ordermgmt.simulators.api;

import com.ordermgmt.simulators.chaos.ChaosProperties;
import com.ordermgmt.simulators.chaos.ChaosState;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Runtime control lever for the chaos profile.
 */
@RestController
@RequestMapping("/chaos")
public class ChaosController {

    private final ChaosState state;

    public ChaosController(ChaosState state) {
        this.state = state;
    }

    @PostMapping("/config")
    public ChaosConfigResponse setConfig(@Valid @RequestBody ChaosConfigRequest request) {
        state.update(request.latencyProbability(), request.rate503(), request.rate429());
        return status();
    }

    @GetMapping("/config")
    public ChaosConfigResponse status() {
        ChaosProperties current = state.current();
        return new ChaosConfigResponse(
                current.latency().probability(),
                current.latency().minMs(),
                current.latency().maxMs(),
                current.rate503(),
                current.rate429());
    }
}
