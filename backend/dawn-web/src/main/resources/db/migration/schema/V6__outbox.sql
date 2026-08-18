CREATE TABLE outbox (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type     VARCHAR(50) NOT NULL,
    reservation_id VARCHAR(36) NOT NULL,
    payload        TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts       INT NOT NULL DEFAULT 0,
    last_error     TEXT,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_outbox_status (status),
    INDEX idx_outbox_reservation (reservation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;