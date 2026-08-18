# Thiết kế Database — Hệ thống đặt vé xem phim (CinePlex)

> Hệ thống monolithic modular (10 module: identity, catalog, cinema, booking, payment, marketing, notification, reporting, ai, web), một database MySQL 8.4 duy nhất, quản lý migration bằng Flyway (V1–V9, `classpath:db/migration`).

---

## 0. Quy ước chung

| Quy ước | Giá trị |
|---|---|
| Engine / Charset | InnoDB, `utf8mb4 / utf8mb4_unicode_ci` |
| Khóa chính | `BIGINT AUTO_INCREMENT` (trừ `reservation` dùng `VARCHAR(36)` do tạo từ Redis/application) |
| Audit timestamp | Mọi bảng có `created_at`, `updated_at` (có bảng thêm `is_deleted`) |
| Soft delete | `is_deleted BOOLEAN DEFAULT FALSE` — không hard-delete (bảo vệ FK, lịch sử) |
| Tiền tệ | `DECIMAL(10,2)` |
| Trạng thái | ENUM trong DB (ràng buộc ngay tại DB) |
| Kiến trúc lưu trữ | Không có bảng `seat` chung cho cả rạp; ghế được tách 2 lớp: **template** (tĩnh, theo phòng) và **instance** (mỗi suất chiếu, động) |

---

## 1. Identity / Auth (`V1__auth.sql`)

### users
Tài khoản người dùng (hỗ trợ admin, moderator, user).

| Cột | Kiểu | Ràng buộc / Ghi chú |
|---|---|---|
| id | BIGINT | PK, auto |
| username | VARCHAR(255) | NOT NULL, UNIQUE |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password | VARCHAR(255) | NOT NULL, bcrypt |
| avatar | VARCHAR(255) | NULL |
| address | VARCHAR(255) | NULL |
| phone | VARCHAR(50) | NULL |
| is_deleted | BOOLEAN | DEFAULT FALSE |
| created_at / updated_at | DATETIME | |

Index: `idx_email`, `idx_username`

### roles
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| name | ENUM('USER','MODERATOR','ADMIN') | NOT NULL, UNIQUE |

### user_role (nhiều-nhiều)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| user_id | BIGINT | FK → users(id) ON DELETE CASCADE |
| role_id | BIGINT | FK → roles(id) ON DELETE CASCADE |
| PK | (user_id, role_id) | |

### refresh_token
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| user_id | BIGINT | FK → users(id) ON DELETE CASCADE |
| token | VARCHAR(512) | NOT NULL, UNIQUE |
| expiry_date | DATETIME | NOT NULL |

---

## 2. Catalog (`V2__catalog.sql`)

### movie
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| title | VARCHAR(255) | NOT NULL |
| original_title | VARCHAR(255) | NULL |
| poster / backdrop | VARCHAR(255) | URL Cloudinary |
| overview | TEXT | |
| duration | INT | NOT NULL (phút) |
| release_date | DATE | |
| imdb_id / film_id | VARCHAR(255) | film_id dùng làm mã nguồn dữ liệu |
| country | VARCHAR(50) | |
| language | ENUM('vi','en') | DEFAULT 'vi' |
| trailer_url | VARCHAR(500) | YouTube URL |
| age_rating | ENUM('P','C13','C16','C18') | Phân loại độ tuổi |
| is_deleted / created_at / updated_at | | |

Index: `idx_title`, `idx_release_date`, `idx_is_deleted`

### genre
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(255) | NOT NULL, UNIQUE |

### movie_genre (nhiều-nhiều)
PK (movie_id, genre_id), cả 2 FK ON DELETE CASCADE.

### review
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| user_id | BIGINT | FK → users ON DELETE CASCADE |
| movie_id | BIGINT | FK → movie ON DELETE CASCADE |
| rating | TINYINT | CHECK 1–5 |
| comment | TEXT | NULL |
| is_deleted | BOOLEAN | |
| UNIQUE (user_id, movie_id) | | 1 user 1 review/phim |

Index: `idx_review_movie_rating (movie_id, rating)`

### article
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| title | VARCHAR(255) | NOT NULL |
| slug | VARCHAR(255) | NOT NULL, UNIQUE |
| summary | TEXT | |
| thumbnail | VARCHAR(255) | |
| content | LONGTEXT | |
| author_id | BIGINT | FK → users ON DELETE SET NULL |
| status | ENUM('DRAFT','PUBLISHED','ARCHIVED') | DEFAULT 'DRAFT' |
| type | ENUM('NEWS','PROMOTION','UNKNOWN') | DEFAULT 'UNKNOWN' |
| views | BIGINT | DEFAULT 0 |

---

## 3. Cinema — rạp, phòng, ghế, suất chiếu (`V3__cinema.sql`)

### theater
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(255) | NOT NULL, UNIQUE |
| location | VARCHAR(255) | |
| is_deleted | BOOLEAN | |

### room (phòng chiếu — thuộc theater)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| theater_id | BIGINT | FK → theater ON DELETE CASCADE |
| name | VARCHAR(100) | NOT NULL |
| room_type | VARCHAR(20) | DEFAULT '2D' (2D/3D/IMAX...) |
| total_seats | INT | DEFAULT 0 — **bảng số** đồng bộ với seat_template còn hoạt động |
| is_deleted | BOOLEAN | |
| UNIQUE (theater_id, name) | | tên phòng duy nhất trong 1 rạp |

### seat_template (bản thiết kế ghế — tĩnh, theo phòng)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| room_id | BIGINT | FK → room ON DELETE CASCADE |
| row_label | VARCHAR(5) | Hàng: A, B, C... |
| seat_number | INT | Số ghế trong hàng: 1, 2, 3... |
| seat_type | ENUM('NORMAL','VIP','COUPLE','WHEELCHAIR') | DEFAULT 'NORMAL' |
| pos_x / pos_y | INT | DEFAULT 0 — tọa độ trên UI seat map |
| is_deleted | BOOLEAN | |
| UNIQUE (room_id, row_label, seat_number) | | vị trí duy nhất |

> Thay đổi layout phòng (thêm/sửa/xóa template) **không** ảnh hưởng suất chiếu đã tạo — seat_instance là bản snapshot tại thời điểm tạo suất chiếu.

### showtime (suất chiếu)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| movie_id | BIGINT | FK → movie ON DELETE CASCADE |
| room_id | BIGINT | FK → room ON DELETE CASCADE (liên kết qua phòng, không qua theater) |
| show_date | DATE | NOT NULL |
| show_time | TIME | NOT NULL |
| end_time | TIME | NULL |
| price | DECIMAL(10,2) | DEFAULT 0 — giá ghế thường |
| vip_price | DECIMAL(10,2) | NULL — giá ghế VIP |

Index: `idx_showtime_movie_id`, `idx_showtime_room_id`, `idx_showtime_date`

---

## 4. Booking — đặt vé, vé, thanh toán (`V5__booking.sql`)

### reservation
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | VARCHAR(36) | PK — sinh ở tầng application/Redis (ORD-...) |
| user_id | BIGINT | FK → users ON DELETE CASCADE |
| showtime_id | BIGINT | FK → showtime ON DELETE CASCADE |
| status | ENUM('PENDING','CONFIRMED','CANCELED','FAILED','EXPIRED','REFUNDED') | DEFAULT 'PENDING' |
| total_amount | DECIMAL(10,2) | NOT NULL DEFAULT 0.00 — giá sau voucher |
| voucher_code | VARCHAR(50) | NULL |
| original_amount | DECIMAL(10,2) | DEFAULT 0 — giá gốc |
| discount_amount | DECIMAL(10,2) | DEFAULT 0 |
| expired_at | DATETIME | NULL — hạn giữ ghế (hold TTL) |
| is_paid | BOOLEAN | DEFAULT FALSE |
| is_deleted | BOOLEAN | |

Index: `idx_reservation_user_id`, `idx_reservation_status`, `idx_reservation_showtime_id`

**State machine:** PENDING (giữ ghế, có `expired_at`) → CONFIRMED (đã thanh toán) | CANCELED (user hủy) | FAILED (thanh toán thất bại) | EXPIRED (quá hạn giữ ghế — ExpirationJob) | REFUNDED (hoàn tiền).

### seat_instance (bản sao ghế cho từng suất chiếu)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| showtime_id | BIGINT | FK → showtime ON DELETE CASCADE |
| seat_template_id | BIGINT | FK → seat_template ON DELETE CASCADE |
| status | ENUM('AVAILABLE','BOOKED','RESERVED') | DEFAULT 'AVAILABLE' |
| reserved_until | DATETIME | NULL |
| reservation_id | VARCHAR(36) | FK → reservation ON DELETE SET NULL |
| price | DECIMAL(10,2) | NOT NULL — chốt giá tại thời điểm tạo suất chiếu |
| UNIQUE (showtime_id, seat_template_id) | | |

Index: `idx_seat_instance_reservation`, `idx_seat_instance_showtime`

> Khi tạo showtime, hệ thống **gen 1 seat_instance cho mỗi seat_template** của phòng (snapshot: vị trí, giá). Concurrency: `bookSeats`/`unbookSeats` là UPDATE có điều kiện (`WHERE status='AVAILABLE'` → 'BOOKED') = **CAS tại DB**; thêm `findByIdWithLock` (PESSIMISTIC_WRITE) cho luồng cần đọc-ghi phức tạp.

### ticket
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| reservation_id | VARCHAR(36) | FK → reservation ON DELETE CASCADE |
| seat_instance_id | BIGINT | FK → seat_instance ON DELETE CASCADE |
| ticket_type | ENUM('NORMAL','VIP','COUPLE') | DEFAULT 'NORMAL' |
| price | DECIMAL(10,2) | NOT NULL |
| UNIQUE (seat_instance_id) | | 1 ghế chỉ có 1 vé |

### payment
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| reservation_id | VARCHAR(36) | NOT NULL (không FK — tách biệt module) |
| payment_intent_id | VARCHAR(255) | NOT NULL — mã intent nội bộ |
| gateway_txn_ref | VARCHAR(255) | NOT NULL, **UNIQUE** — mã giao dịch cổng (chống callback trùng) |
| gateway_response | JSON | NULL — payload thô của cổng |
| amount | DECIMAL(10,2) | DEFAULT 0 |
| method | ENUM('MOMO','VNPAY','ZALOPAY','UNKNOWN') | NOT NULL |
| status | ENUM('PENDING','PAID','FAILED','CANCELED','REFUNDED') | DEFAULT 'PENDING' |
| paid_at | DATETIME | NULL |

Index: `idx_payment_reservation`, `idx_payment_intent`, `idx_payment_status`

---

## 5. Marketing — voucher (`V4__marketing.sql`)

### vouchers (chương trình khuyến mãi — template)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(255) | NOT NULL |
| code | VARCHAR(50) | NOT NULL, UNIQUE |
| start_at / end_at | DATETIME | NOT NULL — thời gian hiệu lực |
| quantity_total / quantity_used | INT | DEFAULT 0 — số lượng phát hành / đã dùng |
| min_order_value | DECIMAL(10,2) | DEFAULT 0 |
| discount_type | ENUM('FIXED','PERCENT') | NOT NULL |
| discount_value | DECIMAL(10,2) | NOT NULL |
| max_discount_amount | DECIMAL(10,2) | NULL — trần giảm khi PERCENT |
| conditions | JSON | NULL — điều kiện mở rộng |
| category | ENUM('CAMPAIGN','SYSTEM') | DEFAULT 'CAMPAIGN' |
| group_ref | VARCHAR(50) | NULL — nhóm voucher |
| status | ENUM('SCHEDULED','ACTIVE','PAUSED','EXPIRED','EXHAUSTED') | DEFAULT 'SCHEDULED' |
| max_per_user | INT | DEFAULT 1 |
| **version** | BIGINT | DEFAULT 0 — **optimistic lock** chống double-claim khi áp dụng voucher |

### user_voucher (voucher đã claim — instance)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| user_id | BIGINT | FK → users ON DELETE CASCADE |
| voucher_id | BIGINT | FK → vouchers ON DELETE CASCADE |
| code | VARCHAR(50) | NOT NULL — snapshot code lúc claim |
| status | ENUM('AVAILABLE','USED','EXPIRED') | DEFAULT 'AVAILABLE' |
| claimed_at | DATETIME | DEFAULT CURRENT_TIMESTAMP |
| used_at | DATETIME | NULL |
| reservation_id | VARCHAR(36) | NULL — gắn với reservation khi dùng |
| expired_at | DATETIME | NOT NULL |

---

## 6. Infrastructure (`V6__outbox.sql`, `V9__audit_log.sql`)

### outbox (transactional outbox pattern)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| event_type | VARCHAR(50) | NOT NULL — VD: PAYMENT_COMPLETED |
| reservation_id | VARCHAR(36) | NOT NULL |
| payload | TEXT | NULL — JSON payload |
| status | VARCHAR(20) | DEFAULT 'PENDING' (PENDING/SENT/FAILED) |
| attempts | INT | DEFAULT 0 |
| last_error | TEXT | NULL |

> Ghi event **cùng transaction** với nghiệp vụ → `OutboxPublisher` (job/thread) đọc PENDING, publish RabbitMQ, đánh dấu SENT. Đảm bảo at-least-once, không mất event khi crash giữa chừng. `ReconciliationJob` quét event lỗi quá hạn.

### audit_log
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT | PK |
| action | VARCHAR(64) | NOT NULL — VD: RESERVATION_HOLD |
| entity | VARCHAR(64) | NOT NULL — VD: RESERVATION |
| entity_id | VARCHAR(64) | NULL |
| actor_id | BIGINT | NULL — user thực hiện |
| from_state | VARCHAR(32) | NULL — trạng thái trước (bắt qua ThreadLocal trước khi mutation) |
| to_state | VARCHAR(32) | NULL — trạng thái sau |
| metadata | VARCHAR(1024) | NULL — SpEL tùy biến, VD: `seats=1,63` |
| created_at | DATETIME | |

> Ghi tự động bằng AOP: annotation `@AuditLog` trên method, ghi **sau khi method thành công** (nghiệp vụ commit), `AuditLogContext` (ThreadLocal) giữ from_state.

---

## 7. Sơ đồ quan hệ

```
users ─┬─< user_role >─ roles
       ├─< refresh_token
       ├─< review >─ movie ─┬─< movie_genre >─ genre
       ├─< article (author)
       ├─< reservation ─┬─< ticket >─ seat_instance ─< seat_template < room < theater
       │                ├─< payment
       │                └─< user_voucher >─ vouchers
       ├─< seat_instance (reservation_id, SET NULL)
       ├─< seat_instance >─ showtime ─< movie, room
       └─< audit_log (actor_id)

reservation <─ outbox (reservation_id, không FK — event log)
```

Điểm đáng chú ý:
- `reservation` là trung tâm: 1 user nhiều reservation, 1 reservation nhiều ticket/payment, có thể dùng 1 user_voucher.
- `showtime` liên kết `room` (không liên kết theater trực tiếp); theater → room → seat_template → seat_instance → showtime.
- `seat_instance` chốt `price` snapshot; `ticket` bảo đảm 1 ghế 1 vé; `payment.gateway_txn_ref` UNIQUE chống callback trùng.
- `vouchers.version` optimistic lock + `user_voucher` bản sao per-user.

---

## 8. Câu hỏi tư vấn tiềm năng (gợi ý mang đi hỏi AI)

1. Đánh giá thiết kế 2 lớp ghế (template/instance) — có nên thêm trường `seat_type` snapshot vào seat_instance để đỡ join với template khi in vé?
2. `reservation` lưu cả `original_amount` + `total_amount` + `discount_amount` nhưng không lưu giá từng ghế — có cần bảng `reservation_item` thay vì chỉ `ticket`?
3. State machine `REFUNDED` chưa có trigger (chưa có luồng hoàn tiền) — đề xuất luồng: payment REFUNDED → outbox → booking đổi status + release ghế + trả voucher.
4. Concurrency: Redis lock (hold) + CAS (book) + PESSIMISTIC_WRITE — còn lỗ hổng nào khi 2 user giữ ghế cùng lúc?
5. `outbox` dùng job poll + RabbitMQ — so với CDC/Debezium thì ổn không ở quy mô này?
6. `payment` không có FK tới reservation (cố ý tách module) — đánh đổi gì về toàn vẹn dữ liệu?
7. `audit_log` chỉ ghi metadata dạng chuỗi — có nên JSON hóa để query được không?