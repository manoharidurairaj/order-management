package com.ordermgmt.loadgen.api;

import com.ordermgmt.loadgen.emit.IngestionClient;
import com.ordermgmt.loadgen.state.LoadGeneratorState;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Load controller to generate baseline load and burst loads.
 */
@RestController
@RequestMapping("/load")
public class LoadController {

    private final LoadGeneratorState state;
    private final IngestionClient ingestionClient;

    public LoadController(LoadGeneratorState state, IngestionClient ingestionClient) {
        this.state = state;
        this.ingestionClient = ingestionClient;
    }

    @PostMapping("/rate")
    public LoadStatusResponse setRate(@Valid @RequestBody SetRateRequest request) {
        state.setBaselineRate(request.ordersPerSecond());
        state.setMaxOrders(request.maxOrders());
        return status();
    }

    @PostMapping("/burst")
    public LoadStatusResponse startBurst(@Valid @RequestBody BurstRequest request) {
        state.startBurst(request.multiplier(), request.durationSeconds());
        return status();
    }

    @PostMapping("/stop")
    public LoadStatusResponse stop() {
        state.clearBurst();
        state.setBaselineRate(0);
        return status();
    }

    @GetMapping("/status")
    public LoadStatusResponse status() {
        return new LoadStatusResponse(
                state.baselineRate(), state.isBursting(), state.effectiveRate(),
                state.maxOrders(), state.ordersSubmitted(),
                ingestionClient.acceptedCount(), ingestionClient.duplicateRejectedCount(), ingestionClient.errorsCount());
    }
}
