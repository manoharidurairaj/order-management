package com.ordermgmt.pipeline.domain;

import com.ordermgmt.common.domain.OrderState;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Order history for audit trail/dashboard usage.
 */
@Entity
@Table(name = "order_history")
public class OrderHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "from_state", length = 32)
    private OrderState fromState;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "to_state", nullable = false, length = 32)
    private OrderState toState;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected OrderHistoryEntity() {
        // JPA
    }

    public OrderHistoryEntity(String orderId, OrderState fromState, OrderState toState, Instant occurredAt) {
        this.orderId = orderId;
        this.fromState = fromState;
        this.toState = toState;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderState getFromState() {
        return fromState;
    }

    public OrderState getToState() {
        return toState;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
