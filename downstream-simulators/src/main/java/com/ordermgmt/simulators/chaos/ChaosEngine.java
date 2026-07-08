package com.ordermgmt.simulators.chaos;

/**
 * Decides what should happen to a call. Deliberately pure (no
 * Thread.sleep, no HTTP) so it's trivial to unit test the probability
 * distribution; executing the decision (actually sleeping) is the caller's
 * job.
 */
public interface ChaosEngine {

    ChaosDecision decide();
}
