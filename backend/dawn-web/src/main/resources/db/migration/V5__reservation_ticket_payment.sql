CREATE TABLE reservation (
    id              VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    showtime_id     BIGINT NOT NULL,
    status          ENUM('PENDING','CONFIRMED','CANCELED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    total_amount    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    voucher_code    VARCHAR(50) NULL,
    original_amount DECIMAL(10,2) DEFAULT 0,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    expired_at      DATETIME NULL,
    is_deleted      BOOLEAN   DEFAULT FALSE,
    created_at      DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservation_user     FOREIGN KEY (user_id)     REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_reservation_showtime FOREIGN KEY (showtime_id) REFERENCES showtime(id) ON DELETE CASCADE,
    INDEX idx_reservation_user_id (user_id),
    INDEX idx_reservation_status (status),
    INDEX idx_reservation_showtime_id (showtime_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE ticket (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id   VARCHAR(36) NOT NULL,
    seat_instance_id BIGINT NOT NULL,
    ticket_type      ENUM('NORMAL','VIP','COUPLE') NOT NULL DEFAULT 'NORMAL',
    price            DECIMAL(10,2) NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_reservation   FOREIGN KEY (reservation_id)   REFERENCES reservation(id)   ON DELETE CASCADE,
    CONSTRAINT fk_ticket_seat_instance FOREIGN KEY (seat_instance_id) REFERENCES seat_instance(id) ON DELETE CASCADE,
    UNIQUE KEY uk_ticket_seat_instance (seat_instance_id),
    INDEX idx_ticket_reservation (reservation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE payment (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id    VARCHAR(36) NOT NULL,
    payment_intent_id VARCHAR(255) NOT NULL,
    gateway_txn_ref   VARCHAR(255) NULL,
    gateway_response  JSON NULL,
    amount            DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    method            ENUM('MOMO','VNPAY','ZALOPAY','UNKNOWN') NOT NULL,
    status            ENUM('PENDING','PAID','FAILED','CANCELED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    paid_at           DATETIME NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payment_reservation (reservation_id),
    INDEX idx_payment_intent (payment_intent_id),
    INDEX idx_payment_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
