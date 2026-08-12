CREATE TABLE movie (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    title          VARCHAR(255) NOT NULL,
    original_title VARCHAR(255),
    poster         VARCHAR(255),
    backdrop       VARCHAR(255),
    overview       TEXT,
    duration       INT NOT NULL,
    release_date   DATE,
    imdb_id        VARCHAR(255),
    film_id        VARCHAR(255),
    country        VARCHAR(50),
    language       ENUM('vi','en') DEFAULT 'vi',
    trailer_url    VARCHAR(500) NULL COMMENT 'YouTube trailer URL',
    age_rating     ENUM('P','C13','C16','C18') NULL COMMENT 'Phân loại độ tuổi',
    is_deleted     BOOLEAN   DEFAULT FALSE,
    created_at     DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_title (title),
    INDEX idx_release_date (release_date),
    INDEX idx_is_deleted (is_deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE genre (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR(255) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE movie_genre (
    movie_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    CONSTRAINT fk_movie_genre FOREIGN KEY (movie_id) REFERENCES movie(id) ON DELETE CASCADE,
    CONSTRAINT fk_genre_movie FOREIGN KEY (genre_id) REFERENCES genre(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

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
