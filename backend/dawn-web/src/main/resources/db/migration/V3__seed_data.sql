-- ============================================================
-- V3__seed_data.sql
-- Flyway seed migration: users + showtime + reservation + seat + payment
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- 1. USERS
-- ────────────────────────────────────────────────────────────
INSERT
    IGNORE
INTO users
(id, username, email, password, avatar, address, phone, is_deleted, created_at, updated_at)
VALUES (3, 'user1', 'user1@example.com',
        '$2a$10$8.4/JMOOazc6T4OQ0kMSiupd3MEqjl2nLNnfo0w17znlyF2sXeVMG',
        'https://i.pravatar.cc/300?img=12', NULL, NULL, 0, NOW(), NOW()),
       (4, 'user2', 'user2@example.com',
        '$2a$10$8.4/JMOOazc6T4OQ0kMSiupd3MEqjl2nLNnfo0w17znlyF2sXeVMG',
        'https://i.pravatar.cc/300?img=12', NULL, NULL, 0, NOW(), NOW()),
       (5, 'user3', 'user3@example.com',
        '$2a$10$8.4/JMOOazc6T4OQ0kMSiupd3MEqjl2nLNnfo0w17znlyF2sXeVMG',
        'https://i.pravatar.cc/300?img=12', NULL, NULL, 0, NOW(), NOW()),
       (6, 'user4', 'user4@example.com',
        '$2a$10$8.4/JMOOazc6T4OQ0kMSiupd3MEqjl2nLNnfo0w17znlyF2sXeVMG',
        'https://i.pravatar.cc/300?img=12', NULL, NULL, 0, NOW(), NOW()),
       (7, 'user5', 'user5@example.com',
        '$2a$10$8.4/JMOOazc6T4OQ0kMSiupd3MEqjl2nLNnfo0w17znlyF2sXeVMG',
        'https://i.pravatar.cc/300?img=12', NULL, NULL, 0, NOW(), NOW());

-- ────────────────────────────────────────────────────────────
-- 2. SHOWTIME  (dynamic — luôn có 3 tháng gần nhất)
-- ────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS sp_seed_showtimes;

DELIMITER
$$

CREATE PROCEDURE sp_seed_showtimes()
BEGIN
    DECLARE
        v_month_offset INT;
    DECLARE
        v_base_date DATE;
    DECLARE
        v_days_in_month INT;
    DECLARE
        v_day INT;
    DECLARE
        v_show_date DATE;

    DELETE
    FROM showtime;

    SET
        v_month_offset = -2;

    WHILE
        v_month_offset <= 0
        DO

            SET v_base_date = DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL v_month_offset MONTH), '%Y-%m-01');
            SET
                v_days_in_month = DAY(LAST_DAY(v_base_date));

            DROP
                TEMPORARY TABLE IF EXISTS tmp_mt_mapping;
            CREATE
                TEMPORARY TABLE tmp_mt_mapping
            (
                movie_id   INT,
                theater_id INT
            );
            INSERT INTO tmp_mt_mapping
            VALUES (1, 1),
                   (1, 2),
                   (1, 3),
                   (2, 3),
                   (2, 4),
                   (2, 5),
                   (3, 1),
                   (3, 2),
                   (3, 5),
                   (4, 1),
                   (4, 4),
                   (5, 1),
                   (5, 2),
                   (5, 3),
                   (6, 2),
                   (6, 4),
                   (6, 5),
                   (7, 3),
                   (7, 5),
                   (8, 1),
                   (8, 2),
                   (9, 3),
                   (9, 4),
                   (10, 5);

            DROP
                TEMPORARY TABLE IF EXISTS tmp_timeslots;
            CREATE
                TEMPORARY TABLE tmp_timeslots
            (
                show_time TIME,
                price     DECIMAL(10, 2)
            );
            INSERT INTO tmp_timeslots
            VALUES ('21:00:00', 150000.00),
                   ('18:00:00', 150000.00),
                   ('14:00:00', 100000.00),
                   ('10:00:00', 100000.00);

            SET
                v_day = 1;
            WHILE
                v_day <= v_days_in_month
                DO
                    SET v_show_date = DATE_ADD(v_base_date, INTERVAL (v_day - 1) DAY);

                    INSERT INTO showtime
                    (movie_id, theater_id, show_date, show_time, price,
                     total_seats, available_seats, created_at, updated_at)
                    SELECT m.movie_id,
                           m.theater_id,
                           v_show_date,
                           t.show_time,
                           t.price,
                           60,
                           8 + FLOOR(RAND() * 24),
                           NOW(),
                           NOW()
                    FROM tmp_mt_mapping m
                             CROSS JOIN tmp_timeslots t;

                    SET
                        v_day = v_day + 1;
                END WHILE;

            SET
                v_month_offset = v_month_offset + 1;
        END WHILE;

    DROP
        TEMPORARY TABLE IF EXISTS tmp_mt_mapping;
    DROP
        TEMPORARY TABLE IF EXISTS tmp_timeslots;
END$$

DELIMITER ;

CALL sp_seed_showtimes();
DROP PROCEDURE IF EXISTS sp_seed_showtimes;

-- ────────────────────────────────────────────────────────────
-- 3. RESERVATION / SEAT / PAYMENT
-- ────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS sp_seed_bookings;

DELIMITER
$$

CREATE PROCEDURE sp_seed_bookings()
BEGIN
    DECLARE
        done INT DEFAULT 0;
    DECLARE
        v_showtime_id BIGINT;
    DECLARE
        v_price DECIMAL(10, 2);
    DECLARE
        v_show_date DATE;

    DECLARE
        v_res_count INT;
    DECLARE
        v_i INT;
    DECLARE
        v_user_id BIGINT;
    DECLARE
        v_res_id VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE
        v_seat_count INT;
    DECLARE
        v_j INT;
    DECLARE
        v_seat_row CHAR(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE
        v_seat_label VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE
        v_total DECIMAL(10, 2);
    DECLARE
        v_method VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE
        v_pay_intent VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE
        v_res_status VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE
        v_pay_status VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE
        v_created DATETIME;
    DECLARE
        v_global_seq INT DEFAULT 0;
    DECLARE
        v_seats_used INT;

    DECLARE
        cur_showtimes CURSOR FOR
            SELECT id, price, show_date
            FROM showtime
            WHERE show_date < CURDATE()
            ORDER BY show_date DESC
            LIMIT 3000;

    DECLARE
        CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    DROP
        TEMPORARY TABLE IF EXISTS tmp_seed_users;
    CREATE
        TEMPORARY TABLE tmp_seed_users
    (
        rn  INT AUTO_INCREMENT PRIMARY KEY,
        uid BIGINT
    );
    INSERT INTO tmp_seed_users (uid)
    SELECT id
    FROM users
    WHERE id BETWEEN 3 AND 8
      AND is_deleted = 0
    ORDER BY id;

    OPEN cur_showtimes;

    read_loop
    :
    LOOP
        FETCH cur_showtimes INTO v_showtime_id, v_price, v_show_date;
        IF
            done THEN
            LEAVE read_loop;
        END IF;

        SET
            v_seats_used = 0;
        SET
            v_res_count = 10 + FLOOR(RAND() * 11);
        SET
            v_i = 1;

        res_loop
:
        WHILE v_i <= v_res_count
            DO

                SET v_seat_count = 1 + FLOOR(RAND() * 4);

                IF
                    (v_seats_used + v_seat_count) > 55 THEN
                    LEAVE res_loop;
                END IF;

                SET
                    v_user_id = (SELECT uid FROM tmp_seed_users ORDER BY RAND() LIMIT 1);
                SET
                    v_res_id = CONCAT('ORD-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 12)));

                IF
                    RAND() < 0.75 THEN
                    SET v_res_status = 'CONFIRMED';
                    SET
                        v_pay_status = 'PAID';
                ELSE
                    SET v_res_status = 'CANCELED';
                    SET
                        v_pay_status = 'CANCELED';
                END IF;

                SET
                    v_total = v_price * v_seat_count;
                SET
                    v_created = DATE_SUB(
                            TIMESTAMP(v_show_date, '09:00:00'),
                            INTERVAL (1 + FLOOR(RAND() * 10)) DAY
                                );

                INSERT INTO reservation
                (id, user_id, showtime_id, status,
                 total_amount, original_amount, discount_amount,
                 is_paid, is_deleted, created_at, updated_at)
                VALUES (v_res_id, v_user_id, v_showtime_id, v_res_status,
                        v_total, v_total, 0.00,
                        IF(v_res_status = 'CONFIRMED', TRUE, FALSE),
                        FALSE, v_created, v_created);

                SET
                    v_j = 1;
                seat_loop
    :
                WHILE v_j <= v_seat_count
                    DO
                        SET @try = 0;
                        SET
                            @ok = 0;

                        find_seat
        :
                        WHILE @try < 40 AND @ok = 0
                            DO
                                SET v_seat_row = ELT(1 + FLOOR(RAND() * 6), 'A', 'B', 'C', 'D', 'E', 'F');
                                SET
                                    v_seat_label = CONCAT(v_seat_row, 1 + FLOOR(RAND() * 10));

                                IF
                                    NOT EXISTS (SELECT 1
                                                FROM seat
                                                WHERE showtime_id = v_showtime_id
                                                  AND seat_number = v_seat_label) THEN
                                    SET @ok = 1;
                                    SET
                                        v_seats_used = v_seats_used + 1;

                                    INSERT INTO seat
                                    (showtime_id, seat_number, status,
                                     reservation_id, created_at, updated_at)
                                    VALUES (v_showtime_id, v_seat_label,
                                            IF(v_res_status = 'CONFIRMED', 'BOOKED', 'AVAILABLE'),
                                            IF(v_res_status = 'CONFIRMED', v_res_id, NULL),
                                            v_created, v_created);
                                END IF;

                                SET
                                    @try = @try + 1;
                            END WHILE
                            find_seat;

                        SET
                            v_j = v_j + 1;
                    END WHILE
                    seat_loop;

                SET
                    v_method = IF(RAND() < 0.5, 'MOMO', 'VNPAY');
                SET
                    v_pay_intent = CONCAT('PAY-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 12)));

                INSERT INTO payment
                (reservation_id, payment_intent_id,
                 amount, method, status, created_at, updated_at)
                VALUES (v_res_id, v_pay_intent, v_total,
                        v_method, v_pay_status, v_created, v_created);

                SET
                    v_i = v_i + 1;
            END WHILE
            res_loop;

    END LOOP
        read_loop;

    CLOSE cur_showtimes;
    DROP
        TEMPORARY TABLE IF EXISTS tmp_seed_users;
END$$

DELIMITER ;

SET FOREIGN_KEY_CHECKS = 0;
DELETE
FROM payment;
DELETE
FROM seat;
DELETE
FROM reservation;
SET FOREIGN_KEY_CHECKS = 1;

CALL sp_seed_bookings();
DROP PROCEDURE IF EXISTS sp_seed_bookings;