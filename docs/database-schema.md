# Database Design — Movie Ticket Booking System (CinePlex)

> Modular monolithic system (10 modules: identity, catalog, cinema, booking, payment, marketing, notification, reporting, ai, web), a single MySQL 8.4 database, migrations managed with Flyway (V1–V9, `classpath:db/migration`).

---

## 0. General Conventions

| Convention | Value |
|---|---|
| Engine / Charset | InnoDB, `utf8mb4 / utf8mb4_unicode_ci` |
| Primary key | `BIGINT AUTO_INCREMENT` (except `reservation`, which uses `VARCHAR(36)` since it is generated from Redis/the application) |
| Audit timestamp | Every table has `created_at`, `updated_at` (some tables also add `is_deleted`) |
| Soft delete | `is_deleted BOOLEAN DEFAULT FALSE` — no hard deletes (protects FKs and history) |
| Currency | `DECIMAL(10,2)` |
| Status | ENUM enforced at the DB level (constraint at the database itself) |
| Storage architecture | There is no single shared `seat` table for the whole theater; seats are split into 2 layers: **template** (static, per room) and **instance** (per showtime, dynamic) |

---

## 1. Identity / Auth (`V1__auth.sql`)

### users
User accounts (supports admin, moderator, user).

| Column | Type | Constraint / Notes |
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
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| name | ENUM('USER','MODERATOR','ADMIN') | NOT NULL, UNIQUE |

### user_role (many-to-many)
| Column | Type | Notes |
|---|---|---|
| user_id | BIGINT | FK → users(id) ON DELETE CASCADE |
| role_id | BIGINT | FK → roles(id) ON DELETE CASCADE |
| PK | (user_id, role_id) | |

### refresh_token
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| user_id | BIGINT | FK → users(id) ON DELETE CASCADE |
| token | VARCHAR(512) | NOT NULL, UNIQUE |
| expiry_date | DATETIME | NOT NULL |

---

## 2. Catalog (`V2__catalog.sql`)

### movie
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| title | VARCHAR(255) | NOT NULL |
| original_title | VARCHAR(255) | NULL |
| poster / backdrop | VARCHAR(255) | Cloudinary URL |
| overview | TEXT | |
| duration | INT | NOT NULL (minutes) |
| release_date | DATE | |
| imdb_id / film_id | VARCHAR(255) | film_id is used as the data source identifier |
| country | VARCHAR(50) | |
| language | ENUM('vi','en') | DEFAULT 'vi' |
| trailer_url | VARCHAR(500) | YouTube URL |
| age_rating | ENUM('P','C13','C16','C18') | Age rating classification |
| is_deleted / created_at / updated_at | | |

Index: `idx_title`, `idx_release_date`, `idx_is_deleted`

### genre
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(255) | NOT NULL, UNIQUE |

### movie_genre (many-to-many)
PK (movie_id, genre_id), both FKs ON DELETE CASCADE.

### review
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| user_id | BIGINT | FK → users ON DELETE CASCADE |
| movie_id | BIGINT | FK → movie ON DELETE CASCADE |
| rating | TINYINT | CHECK 1–5 |
| comment | TEXT | NULL |
| is_deleted | BOOLEAN | |
| UNIQUE (user_id, movie_id) | | 1 review per user per movie |

Index: `idx_review_movie_rating (movie_id, rating)`

### article
| Column | Type | Notes |
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

## 3. Cinema — theaters, rooms, seats, showtimes (`V3__cinema.sql`)

### theater
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(255) | NOT NULL, UNIQUE |
| location | VARCHAR(255) | |
| is_deleted | BOOLEAN | |

### room (screening room — belongs to a theater)
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| theater_id | BIGINT | FK → theater ON DELETE CASCADE |
| name | VARCHAR(100) | NOT NULL |
| room_type | VARCHAR(20) | DEFAULT '2D' (2D/3D/IMAX...) |
| total_seats | INT | DEFAULT 0 — a **denormalized counter** kept in sync with active seat_template rows |
| is_deleted | BOOLEAN | |
| UNIQUE (theater_id, name) | | room name is unique within a theater |

### seat_template (seat layout blueprint — static, per room)
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| room_id | BIGINT | FK → room ON DELETE CASCADE |
| row_label | VARCHAR(5) | Row: A, B, C... |
| seat_number | INT | Seat number within the row: 1, 2, 3... |
| seat_type | ENUM('NORMAL','VIP','COUPLE','WHEELCHAIR') | DEFAULT 'NORMAL' |
| pos_x / pos_y | INT | DEFAULT 0 — coordinates on the UI seat map |
| is_deleted | BOOLEAN | |
| UNIQUE (room_id, row_label, seat_number) | | position must be unique |

> Changing a room's layout (adding/editing/deleting templates) does **not** affect showtimes that already exist — seat_instance is a snapshot taken at the moment the showtime was created.

### showtime
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| movie_id | BIGINT | FK → movie ON DELETE CASCADE |
| room_id | BIGINT | FK → room ON DELETE CASCADE (linked via the room, not directly via the theater) |
| show_date | DATE | NOT NULL |
| show_time | TIME | NOT NULL |
| end_time | TIME | NULL |
| price | DECIMAL(10,2) | DEFAULT 0 — standard seat price |
| vip_price | DECIMAL(10,2) | NULL — VIP seat price |

Index: `idx_showtime_movie_id`, `idx_showtime_room_id`, `idx_showtime_date`

---

## 4. Booking — reservations, tickets, payments (`V5__booking.sql`)

### reservation
| Column | Type | Notes |
|---|---|---|
| id | VARCHAR(36) | PK — generated at the application/Redis layer (ORD-...) |
| user_id | BIGINT | FK → users ON DELETE CASCADE |
| showtime_id | BIGINT | FK → showtime ON DELETE CASCADE |
| status | ENUM('PENDING','CONFIRMED','CANCELED','FAILED','EXPIRED','REFUNDED') | DEFAULT 'PENDING' |
| total_amount | DECIMAL(10,2) | NOT NULL DEFAULT 0.00 — price after voucher discount |
| voucher_code | VARCHAR(50) | NULL |
| original_amount | DECIMAL(10,2) | DEFAULT 0 — original (pre-discount) price |
| discount_amount | DECIMAL(10,2) | DEFAULT 0 |
| expired_at | DATETIME | NULL — seat hold deadline (hold TTL) |
| is_paid | BOOLEAN | DEFAULT FALSE |
| is_deleted | BOOLEAN | |

Index: `idx_reservation_user_id`, `idx_reservation_status`, `idx_reservation_showtime_id`

**State machine:** PENDING (seats held, has `expired_at`) → CONFIRMED (payment completed) | CANCELED (canceled by user) | FAILED (payment failed) | EXPIRED (seat hold expired — ExpirationJob) | REFUNDED (refunded).

### seat_instance (per-showtime seat copy)
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| showtime_id | BIGINT | FK → showtime ON DELETE CASCADE |
| seat_template_id | BIGINT | FK → seat_template ON DELETE CASCADE |
| status | ENUM('AVAILABLE','BOOKED','RESERVED') | DEFAULT 'AVAILABLE' |
| reserved_until | DATETIME | NULL |
| reservation_id | VARCHAR(36) | FK → reservation ON DELETE SET NULL |
| price | DECIMAL(10,2) | NOT NULL — price locked in at the time the showtime was created |
| UNIQUE (showtime_id, seat_template_id) | | |

Index: `idx_seat_instance_reservation`, `idx_seat_instance_showtime`

> When a showtime is created, the system **generates one seat_instance per seat_template** of the room (a snapshot of position and price). Concurrency: `bookSeats`/`unbookSeats` are conditional UPDATEs (`WHERE status='AVAILABLE'` → 'BOOKED') = **CAS at the DB level**; `findByIdWithLock` (PESSIMISTIC_WRITE) is added for flows that need complex read-then-write handling.

### ticket
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| reservation_id | VARCHAR(36) | FK → reservation ON DELETE CASCADE |
| seat_instance_id | BIGINT | FK → seat_instance ON DELETE CASCADE |
| ticket_type | ENUM('NORMAL','VIP','COUPLE') | DEFAULT 'NORMAL' |
| price | DECIMAL(10,2) | NOT NULL |
| UNIQUE (seat_instance_id) | | a seat can only have 1 ticket |

### payment
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| reservation_id | VARCHAR(36) | NOT NULL (no FK — kept decoupled across modules) |
| payment_intent_id | VARCHAR(255) | NOT NULL — internal intent identifier |
| gateway_txn_ref | VARCHAR(255) | NOT NULL, **UNIQUE** — gateway transaction reference (guards against duplicate callbacks) |
| gateway_response | JSON | NULL — raw payload from the gateway |
| amount | DECIMAL(10,2) | DEFAULT 0 |
| method | ENUM('MOMO','VNPAY','ZALOPAY','UNKNOWN') | NOT NULL |
| status | ENUM('PENDING','PAID','FAILED','CANCELED','REFUNDED') | DEFAULT 'PENDING' |
| paid_at | DATETIME | NULL |

Index: `idx_payment_reservation`, `idx_payment_intent`, `idx_payment_status`

---

## 5. Marketing — vouchers (`V4__marketing.sql`)

### vouchers (promotion campaign — template)
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(255) | NOT NULL |
| code | VARCHAR(50) | NOT NULL, UNIQUE |
| start_at / end_at | DATETIME | NOT NULL — validity window |
| quantity_total / quantity_used | INT | DEFAULT 0 — total issued / already used |
| min_order_value | DECIMAL(10,2) | DEFAULT 0 |
| discount_type | ENUM('FIXED','PERCENT') | NOT NULL |
| discount_value | DECIMAL(10,2) | NOT NULL |
| max_discount_amount | DECIMAL(10,2) | NULL — discount cap when PERCENT |
| conditions | JSON | NULL — extended conditions |
| category | ENUM('CAMPAIGN','SYSTEM') | DEFAULT 'CAMPAIGN' |
| group_ref | VARCHAR(50) | NULL — voucher grouping |
| status | ENUM('SCHEDULED','ACTIVE','PAUSED','EXPIRED','EXHAUSTED') | DEFAULT 'SCHEDULED' |
| max_per_user | INT | DEFAULT 1 |
| **version** | BIGINT | DEFAULT 0 — **optimistic lock** guarding against double-claim when applying a voucher |

### user_voucher (claimed voucher — instance)
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| user_id | BIGINT | FK → users ON DELETE CASCADE |
| voucher_id | BIGINT | FK → vouchers ON DELETE CASCADE |
| code | VARCHAR(50) | NOT NULL — snapshot of the code at claim time |
| status | ENUM('AVAILABLE','USED','EXPIRED') | DEFAULT 'AVAILABLE' |
| claimed_at | DATETIME | DEFAULT CURRENT_TIMESTAMP |
| used_at | DATETIME | NULL |
| reservation_id | VARCHAR(36) | NULL — linked to the reservation when used |
| expired_at | DATETIME | NOT NULL |

---

## 6. Infrastructure (`V6__outbox.sql`, `V9__audit_log.sql`)

### outbox (transactional outbox pattern)
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| event_type | VARCHAR(50) | NOT NULL — e.g. PAYMENT_COMPLETED |
| reservation_id | VARCHAR(36) | NOT NULL |
| payload | TEXT | NULL — JSON payload |
| status | VARCHAR(20) | DEFAULT 'PENDING' (PENDING/SENT/FAILED) |
| attempts | INT | DEFAULT 0 |
| last_error | TEXT | NULL |

> The event is written **in the same transaction** as the business operation → `OutboxPublisher` (job/thread) reads PENDING rows, publishes to RabbitMQ, and marks them SENT. This guarantees at-least-once delivery, with no event lost if a crash occurs midway. `ReconciliationJob` scans for events that failed past their retry deadline.

### audit_log
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| action | VARCHAR(64) | NOT NULL — e.g. RESERVATION_HOLD |
| entity | VARCHAR(64) | NOT NULL — e.g. RESERVATION |
| entity_id | VARCHAR(64) | NULL |
| actor_id | BIGINT | NULL — the user who performed the action |
| from_state | VARCHAR(32) | NULL — prior state (captured via ThreadLocal before the mutation) |
| to_state | VARCHAR(32) | NULL — resulting state |
| metadata | VARCHAR(1024) | NULL — custom SpEL expression, e.g. `seats=1,63` |
| created_at | DATETIME | |

> Written automatically via AOP: the `@AuditLog` annotation on a method logs **after the method succeeds** (business transaction committed); `AuditLogContext` (ThreadLocal) holds the from_state.

---

## 7. Relationship Diagram

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

reservation <─ outbox (reservation_id, no FK — event log)
```

Notable points:
- `reservation` is the central entity: 1 user has many reservations, 1 reservation has many tickets/payments, and can use 1 user_voucher.
- `showtime` links to `room` (not directly to theater); theater → room → seat_template → seat_instance → showtime.
- `seat_instance` locks in a `price` snapshot; `ticket` guarantees 1 seat per 1 ticket; `payment.gateway_txn_ref` is UNIQUE to prevent duplicate callbacks.
- `vouchers.version` optimistic lock + `user_voucher` as a per-user copy.

---

## 8. Potential Discussion Questions (suggested prompts to explore further with AI)

1. Evaluate the two-layer seat design (template/instance) — should a snapshotted `seat_type` field be added to seat_instance to avoid joining with the template when printing tickets?
2. `reservation` stores `original_amount` + `total_amount` + `discount_amount` but does not store the per-seat price — is a `reservation_item` table needed instead of relying solely on `ticket`?
3. The `REFUNDED` state in the state machine has no trigger yet (no refund flow implemented) — proposed flow: payment REFUNDED → outbox → booking updates status + releases seats + returns the voucher.
4. Concurrency: Redis lock (hold) + CAS (book) + PESSIMISTIC_WRITE — are there any remaining gaps when two users try to hold the same seat at the same time?
5. `outbox` uses a polling job + RabbitMQ — is this adequate at this scale compared to CDC/Debezium?
6. `payment` has no FK to reservation (intentionally decoupled across modules) — what data-integrity trade-offs does this introduce?
7. `audit_log` only stores metadata as a string — should it be stored as JSON to make it queryable?
