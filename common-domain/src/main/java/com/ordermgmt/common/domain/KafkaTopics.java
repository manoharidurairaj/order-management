package com.ordermgmt.common.domain;

/**
 * Kafka topic names.
 */
public final class KafkaTopics {

    public static final String ORDERS_LIFECYCLE = "orders.lifecycle";
    public static final String ORDERS_DLQ = "orders.dlq";

    private KafkaTopics() {
    }
}
