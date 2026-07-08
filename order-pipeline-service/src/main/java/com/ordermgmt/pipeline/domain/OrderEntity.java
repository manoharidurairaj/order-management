package com.ordermgmt.pipeline.domain;

import com.ordermgmt.common.domain.OrderState;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private String id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private OrderState state;

    // Backbone of the "never process the same order twice" guarantee: any
    // concurrent/duplicate attempt to update this row loses the optimistic
    // lock check and is treated as a no-op redelivery, not an error.
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderEntity() {
        // JPA
    }

    public OrderEntity(String id, String idempotencyKey, String customerId, OrderState state) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.customerId = customerId;
        this.state = state;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderState getState() {
        return state;
    }

    public void setState(OrderState state) {
        this.state = state;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
