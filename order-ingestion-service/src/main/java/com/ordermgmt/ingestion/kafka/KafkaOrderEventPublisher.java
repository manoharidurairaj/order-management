package com.ordermgmt.ingestion.kafka;

import com.ordermgmt.common.domain.KafkaTopics;
import com.ordermgmt.common.domain.OrderCancelRequestedEvent;
import com.ordermgmt.common.domain.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaOrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishOrderPlaced(OrderPlacedEvent event) {
        // Keyed by orderId (domain identity), never by idempotency key —
        // this is what guarantees every event for a given order lands on the
        // same partition and is processed in order downstream.
        log.info("Publishing OrderPlacedEvent orderId={} idempotencyKey={}", event.orderId(), event.idempotencyKey());
        kafkaTemplate.send(KafkaTopics.ORDERS_LIFECYCLE, event.orderId(), event);
    }

    @Override
    public void publishCancelRequested(OrderCancelRequestedEvent event) {
        log.info("Publishing OrderCancelRequestedEvent orderId={}", event.orderId());
        kafkaTemplate.send(KafkaTopics.ORDERS_LIFECYCLE, event.orderId(), event);
    }
}
