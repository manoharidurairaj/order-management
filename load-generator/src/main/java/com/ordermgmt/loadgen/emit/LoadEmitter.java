package com.ordermgmt.loadgen.emit;

import com.ordermgmt.loadgen.generator.GeneratedOrder;
import com.ordermgmt.loadgen.generator.OrderPayloadFactory;
import com.ordermgmt.loadgen.state.LoadGeneratorState;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ticks on a fixed short interval and emits order that the current
 * effective rate calls for.
 */
@Component
public class LoadEmitter {

    private static final long TICK_MILLIS = 100;

    private final LoadGeneratorState state;
    private final OrderPayloadFactory payloadFactory;
    private final IngestionClient ingestionClient;
    private final IdempotencyKeyTracker keyTracker;
    private final ExecutorService dispatchExecutor;

    private double accumulator = 0.0;

    public LoadEmitter(
            LoadGeneratorState state,
            OrderPayloadFactory payloadFactory,
            IngestionClient ingestionClient,
            IdempotencyKeyTracker keyTracker,
            @Value("${ordermgmt.load.dispatch-threads:16}") int dispatchThreads) {
        this.state = state;
        this.payloadFactory = payloadFactory;
        this.ingestionClient = ingestionClient;
        this.keyTracker = keyTracker;
        this.dispatchExecutor = Executors.newFixedThreadPool(dispatchThreads);
    }

    @Scheduled(fixedRate = TICK_MILLIS)
    public void tick() {
        double ratePerSecond = state.effectiveRate();
        if (ratePerSecond <= 0) {
            return;
        }

        accumulator += ratePerSecond * (TICK_MILLIS / 1000.0);
        int toEmit = (int) Math.floor(accumulator);
        accumulator -= toEmit;
        toEmit = state.reserveEmitBudget(toEmit);

        for (int i = 0; i < toEmit; i++) {
            GeneratedOrder order = payloadFactory.randomOrder();
            String idempotencyKey = keyTracker.nextKey();
            dispatchExecutor.submit(() -> ingestionClient.submitOrder(order, idempotencyKey));
        }
    }

    @PreDestroy
    void shutdown() {
        dispatchExecutor.shutdown();
    }
}
