package com.ordermgmt.pipeline.kafka;

import com.ordermgmt.common.domain.OrderCancelRequestedEvent;
import com.ordermgmt.common.domain.OrderPlacedEvent;
import com.ordermgmt.pipeline.orchestration.OrderOrchestrator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.ordermgmt.common.domain.KafkaTopics.ORDERS_LIFECYCLE;

/**
 * Kafka listener to dispatch by event type.
 */
@Component
public class PipelineKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(PipelineKafkaListener.class);

    private final OrderOrchestrator orchestrator;

    public PipelineKafkaListener(OrderOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(
            topics = ORDERS_LIFECYCLE,
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "${ordermgmt.pipeline.consumer-concurrency:3}")
    public void onEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        if (event instanceof OrderPlacedEvent placed) {
            orchestrator.handlePlaced(placed);
        } else if (event instanceof OrderCancelRequestedEvent cancelRequested) {
            orchestrator.handleCancelRequested(cancelRequested);
        } else {
            // OrderStateChangedEvent instances published by this very
            // service also land on this topic (the dashboard consumes
            // them) — nothing for the orchestrator to do with its own
            // broadcasts, so this is an expected no-op, not an error.
            log.debug("Ignoring event of type {}", event.getClass().getSimpleName());
        }
    }
}
