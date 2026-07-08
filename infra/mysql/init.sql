CREATE TABLE IF NOT EXISTS orders (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    customer_id     VARCHAR(64)  NOT NULL,
    state           VARCHAR(32)  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_orders_idempotency_key (idempotency_key)
) ENGINE=InnoDB;

-- Append-only audit trail. Drives dashboard time-in-stage / stuck-order
-- detection and gives a durable record independent of Kafka retention.
CREATE TABLE IF NOT EXISTS order_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    VARCHAR(64)  NOT NULL,
    from_state  VARCHAR(32)  NULL,
    to_state    VARCHAR(32)  NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_order_history_order_id (order_id)
) ENGINE=InnoDB;
