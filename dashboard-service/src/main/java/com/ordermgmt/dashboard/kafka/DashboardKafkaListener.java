package com.ordermgmt.dashboard.kafka;

import com.ordermgmt.common.domain.OrderFailedEvent;
import com.ordermgmt.common.domain.OrderStateChangedEvent;
import com.ordermgmt.dashboard.metrics.DashboardMetricsAggregator;
import com.ordermgmt.dashboard.metrics.DashboardSnapshotService;
import com.ordermgmt.dashboard.stream.DashboardBroadcaster;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static com.ordermgmt.common.domain.KafkaTopics.ORDERS_DLQ;
import static com.ordermgmt.common.domain.KafkaTopics.ORDERS_LIFECYCLE;

@Component
public class DashboardKafkaListener {

    private final DashboardMetricsAggregator aggregator;
    private final DashboardSnapshotService snapshotService;
    private final DashboardBroadcaster broadcaster;

    public DashboardKafkaListener(
            DashboardMetricsAggregator aggregator, DashboardSnapshotService snapshotService, DashboardBroadcaster broadcaster) {
        this.aggregator = aggregator;
        this.snapshotService = snapshotService;
        this.broadcaster = broadcaster;
    }

    @KafkaListener(topics = ORDERS_LIFECYCLE, groupId = "dashboard-service")
    public void onLifecycleEvent(ConsumerRecord<String, Object> record) {
        // Only OrderStateChangedEvent facts are aggregated. OrderPlacedEvent
        // and OrderCancelRequestedEvent are commands the pipeline hasn't
        // necessarily acted on yet; counting them here would double-count
        // against the pipeline's own PLACED OrderStateChangedEvent.
        Object event = record.value();
        if (event instanceof OrderStateChangedEvent stateChanged) {
            aggregator.onStateChanged(stateChanged);
            broadcaster.publish(snapshotService.snapshot());
        }
    }

    @KafkaListener(topics = ORDERS_DLQ, groupId = "dashboard-service")
    public void onDlqEvent(@Payload OrderFailedEvent event) {
        aggregator.onOrderFailed();
        broadcaster.publish(snapshotService.snapshot());
    }
}
