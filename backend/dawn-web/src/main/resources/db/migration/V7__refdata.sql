INSERT IGNORE INTO roles (name) VALUES
    ('USER'),
    ('MODERATOR'),
    ('ADMIN');

INSERT IGNORE INTO genre (name) VALUES
    ('Sci-Fi'),
    ('Adventure'),
    ('Action'),
    ('Fantasy'),
    ('Family'),
    ('Thriller'),
    ('Crime'),
    ('Drama'),
    ('Comedy'),
    ('Animation');

INSERT IGNORE INTO theater (name, location) VALUES
    ('Thanh Xuân', 'Khu đô thị Royal City, Thanh Xuân, Hà Nội'),
    ('Mỹ Đình', 'Khu đô thị Mỹ Đình, Nam Từ Liêm, Hà Nội'),
    ('Cầu Giấy', '236 Cầu Giấy, Hà Nội'),
    ('Hà Đông', 'Hà Đông, Hà Nội'),
    ('Đại Mô', 'Đại Mô, Hà Nội');

INSERT IGNORE INTO room (theater_id, name, total_seats) VALUES
    (1, 'Rạp 1', 60),
    (1, 'Rạp 2', 40),
    (2, 'Rạp 1', 60),
    (2, 'Rạp 2', 50),
    (3, 'Rạp 1', 80),
    (3, 'Rạp 2', 60),
    (4, 'Rạp 1', 60),
    (5, 'Rạp 1', 60);

INSERT IGNORE INTO seat_template (room_id, row_label, seat_number, seat_type, pos_x, pos_y) VALUES
    (1, 'A', 1, 'NORMAL', 0, 0), (1, 'A', 2, 'NORMAL', 1, 0), (1, 'A', 3, 'NORMAL', 2, 0), (1, 'A', 4, 'NORMAL', 3, 0), (1, 'A', 5, 'NORMAL', 4, 0),
    (1, 'A', 6, 'NORMAL', 5, 0), (1, 'A', 7, 'NORMAL', 6, 0), (1, 'A', 8, 'NORMAL', 7, 0), (1, 'A', 9, 'NORMAL', 8, 0), (1, 'A', 10, 'NORMAL', 9, 0),
    (1, 'B', 1, 'NORMAL', 0, 1), (1, 'B', 2, 'NORMAL', 1, 1), (1, 'B', 3, 'NORMAL', 2, 1), (1, 'B', 4, 'NORMAL', 3, 1), (1, 'B', 5, 'NORMAL', 4, 1),
    (1, 'B', 6, 'NORMAL', 5, 1), (1, 'B', 7, 'NORMAL', 6, 1), (1, 'B', 8, 'NORMAL', 7, 1), (1, 'B', 9, 'NORMAL', 8, 1), (1, 'B', 10, 'NORMAL', 9, 1),
    (1, 'C', 1, 'NORMAL', 0, 2), (1, 'C', 2, 'NORMAL', 1, 2), (1, 'C', 3, 'NORMAL', 2, 2), (1, 'C', 4, 'NORMAL', 3, 2), (1, 'C', 5, 'NORMAL', 4, 2),
    (1, 'C', 6, 'NORMAL', 5, 2), (1, 'C', 7, 'NORMAL', 6, 2), (1, 'C', 8, 'NORMAL', 7, 2), (1, 'C', 9, 'NORMAL', 8, 2), (1, 'C', 10, 'NORMAL', 9, 2),
    (1, 'D', 1, 'VIP', 0, 3), (1, 'D', 2, 'VIP', 1, 3), (1, 'D', 3, 'VIP', 2, 3), (1, 'D', 4, 'VIP', 3, 3), (1, 'D', 5, 'VIP', 4, 3),
    (1, 'D', 6, 'VIP', 5, 3), (1, 'D', 7, 'VIP', 6, 3), (1, 'D', 8, 'VIP', 7, 3), (1, 'D', 9, 'VIP', 8, 3), (1, 'D', 10, 'VIP', 9, 3),
    (1, 'E', 1, 'VIP', 0, 4), (1, 'E', 2, 'VIP', 1, 4), (1, 'E', 3, 'VIP', 2, 4), (1, 'E', 4, 'VIP', 3, 4), (1, 'E', 5, 'VIP', 4, 4),
    (1, 'E', 6, 'VIP', 5, 4), (1, 'E', 7, 'VIP', 6, 4), (1, 'E', 8, 'VIP', 7, 4), (1, 'E', 9, 'VIP', 8, 4), (1, 'E', 10, 'VIP', 9, 4);

INSERT IGNORE INTO seat_template (room_id, row_label, seat_number, seat_type, pos_x, pos_y) SELECT 2, st.row_label, st.seat_number, st.seat_type, st.pos_x, st.pos_y FROM seat_template st WHERE st.room_id = 1 LIMIT 40;
INSERT IGNORE INTO seat_template (room_id, row_label, seat_number, seat_type, pos_x, pos_y) SELECT 3, st.row_label, st.seat_number, st.seat_type, st.pos_x, st.pos_y FROM seat_template st WHERE st.room_id = 1 LIMIT 60;
INSERT IGNORE INTO seat_template (room_id, row_label, seat_number, seat_type, pos_x, pos_y) SELECT 4, st.row_label, st.seat_number, st.seat_type, st.pos_x, st.pos_y FROM seat_template st WHERE st.room_id = 1 LIMIT 50;
INSERT IGNORE INTO seat_template (room_id, row_label, seat_number, seat_type, pos_x, pos_y) SELECT 5, st.row_label, st.seat_number, st.seat_type, st.pos_x, st.pos_y FROM seat_template st WHERE st.room_id = 1 LIMIT 80;
INSERT IGNORE INTO seat_template (room_id, row_label, seat_number, seat_type, pos_x, pos_y) SELECT 6, st.row_label, st.seat_number, st.seat_type, st.pos_x, st.pos_y FROM seat_template st WHERE st.room_id = 1 LIMIT 60;
INSERT IGNORE INTO seat_template (room_id, row_label, seat_number, seat_type, pos_x, pos_y) SELECT 7, st.row_label, st.seat_number, st.seat_type, st.pos_x, st.pos_y FROM seat_template st WHERE st.room_id = 1 LIMIT 60;
INSERT IGNORE INTO seat_template (room_id, row_label, seat_number, seat_type, pos_x, pos_y) SELECT 8, st.row_label, st.seat_number, st.seat_type, st.pos_x, st.pos_y FROM seat_template st WHERE st.room_id = 1 LIMIT 60;
