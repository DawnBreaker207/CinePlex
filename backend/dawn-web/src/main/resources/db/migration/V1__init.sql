CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    username   VARCHAR(255) NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    avatar     VARCHAR(255),
    address    VARCHAR(50),
    phone      VARCHAR(50),
    is_deleted BOOLEAN               DEFAULT FALSE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    name       ENUM ('USER', 'MODERATOR','ADMIN') NOT NULL UNIQUE,
    created_at DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_role
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS movie
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    title          VARCHAR(255) NOT NULL,
    original_title VARCHAR(255),
    poster         VARCHAR(255),
    backdrop       VARCHAR(255),
    overview       TEXT,
    duration       INT          NOT NULL,
    release_date   DATE,
    imdb_id        VARCHAR(255),
    film_id        VARCHAR(255),
    country        VARCHAR(50),
    language       ENUM ('vi', 'en')     DEFAULT 'vi',
    is_deleted     BOOLEAN               DEFAULT FALSE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_title (title),
    INDEX idx_release_date (release_date),
    index idx_is_deleted (is_deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS genre
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR(255) NOT NULL UNIQUE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS movie_genre
(
    movie_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    CONSTRAINT fk_movie_genre FOREIGN KEY (movie_id) REFERENCES movie (id) ON DELETE CASCADE,
    CONSTRAINT fk_genre_movie FOREIGN KEY (genre_id) REFERENCES genre (id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS theater
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR(255) NOT NULL UNIQUE,
    location   VARCHAR(255),
    capacity   INT          NOT NULL,
    is_deleted BOOLEAN               DEFAULT FALSE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_is_deleted (is_deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS showtime
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id        BIGINT         NOT NULL,
    theater_id      BIGINT         NOT NULL,
    show_date       DATE           NOT NULL,
    show_time       TIME           NOT NULL,
    price           DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_seats     INT            NOT NULL,
    available_seats INT            NOT NULL,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_movie_showtime FOREIGN KEY (movie_id) REFERENCES movie (id) ON DELETE CASCADE,
    CONSTRAINT fk_theater_showtime FOREIGN KEY (theater_id) REFERENCES theater (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reservation
(
    id              VARCHAR(50)    NOT NULL PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    showtime_id     BIGINT         NOT NULL,
    status          ENUM ('CONFIRMED','CANCELED'),
    total_amount    DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    # Voucher
    voucher_code    VARCHAR(50)    NULL COMMENT 'Voucher Used',
    original_amount DECIMAL(10, 2)          DEFAULT 0 COMMENT 'Total Original',
    discount_amount DECIMAL(10, 2)          DEFAULT 0 COMMENT 'Total Discount',
    is_paid         BOOLEAN                 DEFAULT FALSE,
    is_deleted      BOOLEAN                 DEFAULT FALSE,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_showtime FOREIGN KEY (showtime_id) REFERENCES showtime (id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_showtime_id (showtime_id),
    INDEX idx_is_deleted (is_deleted)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS seat
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    showtime_id    BIGINT      NOT NULL,
    seat_number    VARCHAR(20) NOT NULL,
    status         ENUM ('AVAILABLE', 'BOOKED') DEFAULT 'AVAILABLE',
    reservation_id VARCHAR(50),
    created_at     DATETIME    NOT NULL         DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_seat_showtime FOREIGN KEY (showtime_id) REFERENCES showtime (id) ON DELETE CASCADE,
    CONSTRAINT fk_seat_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id) ON DELETE SET NULL,
    UNIQUE (showtime_id, seat_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id    VARCHAR(50) UNIQUE,
    payment_intent_id VARCHAR(255)   NOT NULL UNIQUE,
    amount            DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    method            ENUM ('MOMO', 'VNPAY'),
    status            ENUM ('PAID', 'CANCELED'),
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment FOREIGN KEY (reservation_id) REFERENCES reservation (id) ON DELETE SET NULL,
    INDEX idx_reservation_id (reservation_id),
    INDEX idx_status (status),
    INDEX idx_payment_intent (payment_intent_id)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS refresh_token
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expiry_date DATETIME     NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_refresh FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS article
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    title      VARCHAR(255) NOT NULL,
    slug       VARCHAR(255) NOT NULL UNIQUE,
    summary    TEXT,
    thumbnail  VARCHAR(255),
    content    LONGTEXT,
    author_id  BIGINT,
    status     ENUM ('DRAFT', 'PUBLISHED','ARCHIVED') DEFAULT 'DRAFT',
    type       ENUM ('NEWS', 'PROMOTION','UNKNOWN')   DEFAULT 'UNKNOWN',
    views      BIGINT                                 DEFAULT 0,
    is_deleted BOOLEAN                                DEFAULT FALSE,
    created_at DATETIME     NOT NULL                  DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL                  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_article_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS vouchers
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    name                VARCHAR(255)             NOT NULL COMMENT 'Name voucher: MA GIAM GIA',
    code                VARCHAR(50)              NOT NULL UNIQUE COMMENT 'Code voucher: MAGIAMGIA',
    start_at            DATETIME                 NOT NULL COMMENT 'Campaign start',
    end_at              DATETIME                 NOT NULL COMMENT 'Campaign end',
    quantity_total      BIGINT                   NOT NULL DEFAULT 0 COMMENT 'Total discount can be use',
    quantity_used       BIGINT                   NOT NULL DEFAULT 0 COMMENT 'Total discount was used',
    min_order_value     BIGINT                   NOT NULL DEFAULT 0 COMMENT 'Minimum booking can be used',
    discount_type       ENUM ('FIXED','PERCENT') NOT NULL COMMENT 'Discount type',
    discount_value      BIGINT                   NOT NULL COMMENT 'Discount value',
    max_discount_amount BIGINT                            DEFAULT NULL COMMENT 'Max discount',
    conditions          JSON                     NULL,
    category            ENUM ('CAMPAIGN','SYSTEM')        DEFAULT 'CAMPAIGN' COMMENT 'voucher type',
    group_ref           VARCHAR(50)              NULL COMMENT 'Voucher group',
    is_active           BOOLEAN                           DEFAULT TRUE COMMENT 'Active voucher',
    version             BIGINT                   NOT NULL DEFAULT 0,
    created_at          DATETIME                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME                 NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_voucher_validity (code, is_active, start_at, end_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;