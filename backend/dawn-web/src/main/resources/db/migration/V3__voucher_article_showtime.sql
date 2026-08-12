CREATE TABLE vouchers (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    name                VARCHAR(255) NOT NULL,
    code                VARCHAR(50)  NOT NULL UNIQUE,
    start_at            DATETIME     NOT NULL,
    end_at              DATETIME     NOT NULL,
    quantity_total      INT          NOT NULL DEFAULT 0,
    quantity_used       INT          NOT NULL DEFAULT 0,
    min_order_value     DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount_type       ENUM('FIXED','PERCENT') NOT NULL,
    discount_value      DECIMAL(10,2) NOT NULL,
    max_discount_amount DECIMAL(10,2) NULL,
    conditions          JSON         NULL,
    category            ENUM('CAMPAIGN','SYSTEM') DEFAULT 'CAMPAIGN',
    group_ref           VARCHAR(50)  NULL,
    status              ENUM('SCHEDULED','ACTIVE','PAUSED','EXPIRED','EXHAUSTED') NOT NULL DEFAULT 'SCHEDULED',
    max_per_user        INT          NOT NULL DEFAULT 1,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_voucher_validity (code, start_at, end_at),
    INDEX idx_voucher_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_voucher (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id        BIGINT NOT NULL,
    voucher_id     BIGINT NOT NULL,
    code           VARCHAR(50) NOT NULL,
    status         ENUM('AVAILABLE','USED','EXPIRED') NOT NULL DEFAULT 'AVAILABLE',
    claimed_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at        DATETIME NULL,
    reservation_id VARCHAR(36) NULL,
    expired_at     DATETIME NOT NULL,
    CONSTRAINT fk_uv_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_uv_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE,
    INDEX idx_uv_user (user_id),
    INDEX idx_uv_status (status),
    INDEX idx_uv_reservation (reservation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE article (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    title      VARCHAR(255) NOT NULL,
    slug       VARCHAR(255) NOT NULL UNIQUE,
    summary    TEXT,
    thumbnail  VARCHAR(255),
    content    LONGTEXT,
    author_id  BIGINT,
    status     ENUM('DRAFT','PUBLISHED','ARCHIVED') DEFAULT 'DRAFT',
    type       ENUM('NEWS','PROMOTION','UNKNOWN')   DEFAULT 'UNKNOWN',
    views      BIGINT   DEFAULT 0,
    is_deleted BOOLEAN  DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_article_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE showtime (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id   BIGINT NOT NULL,
    room_id    BIGINT NOT NULL,
    show_date  DATE NOT NULL,
    show_time  TIME NOT NULL,
    price      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    vip_price  DECIMAL(10,2) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_showtime_movie FOREIGN KEY (movie_id) REFERENCES movie(id) ON DELETE CASCADE,
    CONSTRAINT fk_showtime_room  FOREIGN KEY (room_id)   REFERENCES room(id)   ON DELETE CASCADE,
    INDEX idx_showtime_movie_id (movie_id),
    INDEX idx_showtime_room_id (room_id),
    INDEX idx_showtime_date (show_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
