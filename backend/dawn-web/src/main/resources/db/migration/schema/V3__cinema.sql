CREATE TABLE theater (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR(255) NOT NULL UNIQUE,
    location   VARCHAR(255),
    is_deleted BOOLEAN   DEFAULT FALSE,
    created_at DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_is_deleted (is_deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE room (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    theater_id  BIGINT NOT NULL,
    name        VARCHAR(100) NOT NULL,
    room_type   VARCHAR(20) NOT NULL DEFAULT '2D',
    total_seats INT NOT NULL DEFAULT 0,
    is_deleted  BOOLEAN   DEFAULT FALSE,
    created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_theater FOREIGN KEY (theater_id) REFERENCES theater(id) ON DELETE CASCADE,
    UNIQUE KEY uk_room_theater_name (theater_id, name),
    INDEX idx_room_theater_id (theater_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE seat_template (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id     BIGINT NOT NULL,
    row_label   VARCHAR(5)  NOT NULL COMMENT 'Hàng ghế: A, B, C...',
    seat_number INT         NOT NULL COMMENT 'Số ghế trong hàng: 1, 2, 3...',
    seat_type   ENUM('NORMAL','VIP','COUPLE','WHEELCHAIR') NOT NULL DEFAULT 'NORMAL',
    pos_x       INT NOT NULL DEFAULT 0 COMMENT 'Tọa độ X trên UI seat map',
    pos_y       INT NOT NULL DEFAULT 0 COMMENT 'Tọa độ Y trên UI seat map',
    is_deleted  BOOLEAN   DEFAULT FALSE,
    created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_seat_template_room FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE,
    UNIQUE KEY uk_seat_room_position (room_id, row_label, seat_number),
    INDEX idx_seat_template_room (room_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE showtime (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id   BIGINT NOT NULL,
    room_id    BIGINT NOT NULL,
    show_date  DATE NOT NULL,
    show_time  TIME NOT NULL,
    end_time   TIME NULL,
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