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