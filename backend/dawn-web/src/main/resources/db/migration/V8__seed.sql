INSERT IGNORE INTO users (username, email, password, phone, address) VALUES
    ('user1', 'user1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0123456789', 'Hà Nội'),
    ('user2', 'user2@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0123456790', 'Hồ Chí Minh'),
    ('user3', 'user3@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0123456791', 'Đà Nẵng'),
    ('user4', 'user4@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0123456792', 'Hải Phòng'),
    ('user5', 'user5@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0123456793', 'Cần Thơ');

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'user1' AND r.name = 'USER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'user2' AND r.name = 'USER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'user3' AND r.name = 'USER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'user4' AND r.name = 'USER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'user5' AND r.name = 'USER';

INSERT IGNORE INTO movie (id, title, original_title, duration, release_date, language, trailer_url, age_rating) VALUES
    (1, 'Siêu nhân', 'Superman', 143, '2025-07-11', 'en', 'https://www.youtube.com/watch?v=example1', 'C13'),
    (2, 'Hành trình thuần hóa rồng', 'How to Train Your Dragon', 104, '2025-06-13', 'en', 'https://www.youtube.com/watch?v=example2', 'P'),
    (3, 'Cướp biển Caribbean', 'Pirates of the Caribbean', 143, '2025-05-24', 'en', 'https://www.youtube.com/watch?v=example3', 'C13'),
    (4, 'V for Vandetta', 'V for Vendetta', 132, '2025-03-17', 'en', 'https://www.youtube.com/watch?v=example4', 'C16'),
    (5, 'Biệt đội siêu anh hùng: Hồi kết', 'Avengers: Endgame', 181, '2025-04-26', 'en', 'https://www.youtube.com/watch?v=example5', 'C13'),
    (6, 'Người sắt', 'Iron Man', 126, '2025-05-02', 'en', 'https://www.youtube.com/watch?v=example6', 'C13'),
    (7, 'Người dơi: Khởi đầu', 'Batman Begins', 140, '2025-06-15', 'en', 'https://www.youtube.com/watch?v=example7', 'C13'),
    (8, 'Ma trận', 'The Matrix', 136, '2025-03-31', 'en', 'https://www.youtube.com/watch?v=example8', 'C16'),
    (9, 'John Wick', 'John Wick', 101, '2025-10-24', 'en', 'https://www.youtube.com/watch?v=example9', 'C18'),
    (10, 'Câu chuyện Lego', 'The Lego Movie', 100, '2025-02-07', 'en', 'https://www.youtube.com/watch?v=example10', 'P');

INSERT IGNORE INTO movie_genre (movie_id, genre_id) VALUES
    (1, 1), (1, 3), (2, 2), (2, 4), (2, 5), (3, 2), (3, 3), (4, 6), (4, 8), (5, 1), (5, 3), (5, 8), (6, 1), (6, 3), (7, 3), (7, 8), (8, 1), (8, 3), (9, 3), (9, 6), (10, 10), (10, 5);

INSERT IGNORE INTO vouchers (name, code, start_at, end_at, quantity_total, min_order_value, discount_type, discount_value, max_discount_amount, category, status, max_per_user) VALUES
    ('Giảm 10% đơn đầu', 'DAWN10', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 100, 50000, 'PERCENT', 10.00, 50000, 'CAMPAIGN', 'ACTIVE', 1),
    ('Giảm 20% cuối tuần', 'WEEKEND20', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 50, 100000, 'PERCENT', 20.00, 100000, 'CAMPAIGN', 'ACTIVE', 1),
    ('50k cho đơn 200k', 'SAVE50', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 200, 200000, 'FIXED', 50000, NULL, 'CAMPAIGN', 'ACTIVE', 1);

INSERT IGNORE INTO article (title, slug, summary, content, author_id, status, type) VALUES
    ('CinePlex khai trương rạp mới', 'cineplex-khai-truong-rap-moi', 'CinePlex mở rộng hệ thống với rạp chiếu phim hiện đại tại Hà Nội.', '<p>CinePlex hân hạnh thông báo khai trương rạp chiếu phim thứ 6 tại Hà Nội...</p>', 1, 'PUBLISHED', 'NEWS'),
    ('Khuyến mãi tháng 7', 'khuyen-mai-thang-7', 'Giảm giá vé xem phim lên đến 20% trong tháng 7.', '<p>Trong tháng 7 này, CinePlex dành tặng ưu đãi đặc biệt...</p>', 1, 'PUBLISHED', 'PROMOTION');

INSERT IGNORE INTO showtime (movie_id, room_id, show_date, show_time, price, vip_price)
SELECT
    m.id,
    st.room_id,
    DATE_SUB(CURDATE(), INTERVAL seq.day DAY) AS show_date,
    t.show_time,
    ROUND(RAND() * 50000 + 50000, -3) AS price,
    ROUND(RAND() * 50000 + 80000, -3) AS vip_price
FROM movie m
CROSS JOIN (
    SELECT 0 AS day UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
    UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12
    UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18
    UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
    UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
) seq
CROSS JOIN (
    SELECT '08:00:00' AS show_time UNION SELECT '10:30:00' UNION SELECT '13:00:00'
    UNION SELECT '15:30:00' UNION SELECT '18:00:00' UNION SELECT '20:30:00'
) t
CROSS JOIN (
    SELECT DISTINCT room_id FROM seat_template
) st
WHERE m.id <= 10 AND seq.day < 30 AND RAND() > 0.4
LIMIT 500;

INSERT IGNORE INTO seat_instance (showtime_id, seat_template_id, status, price)
SELECT s.id, st.id, 'AVAILABLE', IF(st.seat_type = 'VIP', s.vip_price, s.price)
FROM showtime s
JOIN seat_template st ON st.room_id = s.room_id;
