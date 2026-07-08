package com.ordermgmt.dashboard.loadgen;

public record LoadGeneratorStats(long accepted, long duplicateRejected, long errors) {

    public static final LoadGeneratorStats UNKNOWN = new LoadGeneratorStats(0, 0, 0);
}
