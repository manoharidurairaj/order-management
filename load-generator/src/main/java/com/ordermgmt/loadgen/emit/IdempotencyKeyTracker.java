package com.ordermgmt.loadgen.emit;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Remembers recently issued idempotency keys so the emitter can
 * deliberately replay one every so often — the demo should be able to show
 * the 409 dedup path actually firing, not just take it on faith.
 */
@Component
public class IdempotencyKeyTracker {

    private static final int HISTORY_SIZE = 200;
    private static final double REPLAY_PROBABILITY = 0.03;

    private final ConcurrentLinkedDeque<String> recentKeys = new ConcurrentLinkedDeque<>();

    public String nextKey() {
        if (!recentKeys.isEmpty() && ThreadLocalRandom.current().nextDouble() < REPLAY_PROBABILITY) {
            String replayed = recentKeys.peekFirst();
            if (replayed != null) {
                return replayed;
            }
        }
        String fresh = UUID.randomUUID().toString();
        recentKeys.addLast(fresh);
        while (recentKeys.size() > HISTORY_SIZE) {
            recentKeys.pollFirst();
        }
        return fresh;
    }
}
