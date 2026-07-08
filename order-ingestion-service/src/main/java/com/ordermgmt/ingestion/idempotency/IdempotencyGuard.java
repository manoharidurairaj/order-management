package com.ordermgmt.ingestion.idempotency;

/**
 * Dedup boundary for client-side double submission (e.g. a customer's app
 * retrying a POST after a timeout).
 */
public interface IdempotencyGuard {

    /**
     * @return true if this is the first time {@code key} has been seen
     * (caller should proceed), false if it's a duplicate (caller should
     * reject).
     */
    boolean tryAcquire(String key);
}
