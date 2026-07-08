package com.ordermgmt.pipeline.kafka;

import com.ordermgmt.common.domain.KafkaTopics;
import com.ordermgmt.common.domain.OrderFailedEvent;
import com.ordermgmt.common.domain.OrderStateChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaPipelineEventPublisher implements PipelineEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPipelineEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaPipelineEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishStateChanged(OrderStateChangedEvent event) {
        // Always keyed by orderId — this is what keeps every event for a
        // given order on the same partition, which is what makes per-order
        // ordering a partition-assignment guarantee rather than something
        // the consumer has to work for.
        kafkaTemplate.send(KafkaTopics.ORDERS_LIFECYCLE, event.orderId(), event);
    }

    @Override
    public void publishFailed(OrderFailedEvent event) {
        log.warn("orderId={} routed to DLQ, reason={}", event.orderId(), event.reason());
        kafkaTemplate.send(KafkaTopics.ORDERS_DLQ, event.orderId(), event);
    }
}
