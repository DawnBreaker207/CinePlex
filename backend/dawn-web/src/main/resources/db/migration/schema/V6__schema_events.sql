-- Messaging & audit: outbox (generic), processed_events (consumer dedup), audit_log
CREATE TABLE outbox (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type     VARCHAR(50)  NOT NULL,
    aggregate_type VARCHAR(30)  NOT NULL DEFAULT 'payment',
    exchange       VARCHAR(100) NOT NULL DEFAULT 'payment.exchange',
    routing_key    VARCHAR(100) NOT NULL DEFAULT 'payment.completed',
    aggregate_id   VARCHAR(50)  NOT NULL,
    payload        TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts       INT NOT NULL DEFAULT 0,
    last_error     TEXT,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version        BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY ux_outbox_event_aggregate (event_type, aggregate_id),
    INDEX idx_outbox_status (status),
    INDEX idx_outbox_aggregate (aggregate_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE processed_events (
    event_id     VARCHAR(64) NOT NULL PRIMARY KEY,
    processed_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE audit_log (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    action     VARCHAR(64)  NOT NULL,
    entity     VARCHAR(64)  NOT NULL,
    entity_id  VARCHAR(64)  NULL,
    actor_id   BIGINT       NULL,
    from_state VARCHAR(32)  NULL,
    to_state   VARCHAR(32)  NULL,
    metadata   VARCHAR(4096) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status     VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    ip_address VARCHAR(64)  NULL,
    old_value  TEXT NULL,
    new_value  TEXT NULL,
    INDEX idx_audit_entity (entity, entity_id),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
