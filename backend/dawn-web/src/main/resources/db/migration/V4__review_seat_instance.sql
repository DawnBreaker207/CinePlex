CREATE TABLE review (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    movie_id   BIGINT NOT NULL,
    rating     TINYINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment    TEXT NULL,
    is_deleted BOOLEAN   DEFAULT FALSE,
    created_at DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_user  FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_review_movie FOREIGN KEY (movie_id) REFERENCES movie(id) ON DELETE CASCADE,
    UNIQUE KEY uk_review_user_movie (user_id, movie_id),
    INDEX idx_review_movie_rating (movie_id, rating)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE seat_instance (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    showtime_id      BIGINT NOT NULL,
    seat_template_id BIGINT NOT NULL,
    status           ENUM('AVAILABLE','BOOKED','RESERVED') NOT NULL DEFAULT 'AVAILABLE',
    reserved_until   DATETIME NULL,
    reservation_id   VARCHAR(36) NULL,
    price            DECIMAL(10,2) NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_seat_instance_showtime  FOREIGN KEY (showtime_id)      REFERENCES showtime(id)      ON DELETE CASCADE,
    CONSTRAINT fk_seat_instance_template  FOREIGN KEY (seat_template_id) REFERENCES seat_template(id) ON DELETE CASCADE,
    UNIQUE KEY uk_seat_showtime (showtime_id, seat_template_id),
    INDEX idx_seat_instance_reservation (reservation_id),
    INDEX idx_seat_instance_showtime (showtime_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
