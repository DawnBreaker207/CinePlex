-- Revenue demo seed: reservations/tickets/payments spread across theaters (last 30 days).
-- Dirty-by-design distribution to exercise report filters: ~88% PAID, ~5% REFUNDED, ~7% FAILED
-- (only PAID counts as collected revenue). Idempotent: only touches reservation_code LIKE 'CPXSEED%'.
-- CHECK-safe on INSERT: is_paid follows status, only PAID carries paid_at.

-- 1. Reservations: up to 3 orders per recent showtime (code = CPXSEED + showtime + slot)
INSERT INTO reservation (reservation_code, user_id, showtime_id, status, total_amount,
                         original_amount, discount_amount, is_paid, is_deleted, created_at)
SELECT x.reservation_code, x.user_id, x.showtime_id, x.status, 0, 0, 0,
       (x.status = 'CONFIRMED'), FALSE, x.created_at
FROM (
    SELECT
        CONCAT('CPXSEED', LPAD(st.id, 6, '0'), 'U', sl.seq) AS reservation_code,
        u.id AS user_id,
        st.id AS showtime_id,
        CASE WHEN MOD(st.id + sl.seq, 10) < 9 THEN 'CONFIRMED' ELSE 'EXPIRED' END AS status,
        DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) AS created_at
    FROM (
        SELECT id, ROW_NUMBER() OVER (ORDER BY RAND()) AS rn
        FROM showtime
        WHERE show_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 30 DAY) AND CURDATE()
    ) st
    CROSS JOIN (
        SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3
    ) sl
    JOIN users u ON u.username = CONCAT('user', 1 + ((st.rn + sl.seq) MOD 5))
    WHERE RAND() < 0.75
) x;

-- 2. Assign first 2 AVAILABLE seats to each CONFIRMED reservation
UPDATE seat_instance si
JOIN (
    SELECT seat.si_id, rv.rid
    FROM (
        SELECT si2.id AS si_id, si2.showtime_id,
               ROW_NUMBER() OVER (PARTITION BY si2.showtime_id ORDER BY si2.id) - 1 AS sidx
        FROM seat_instance si2
        WHERE si2.status = 'AVAILABLE'
    ) seat
    JOIN (
        SELECT r.id AS rid, r.showtime_id,
               ROW_NUMBER() OVER (PARTITION BY r.showtime_id ORDER BY r.id) - 1 AS ridx
        FROM reservation r
        WHERE r.reservation_code LIKE 'CPXSEED%' AND r.status = 'CONFIRMED'
    ) rv ON rv.showtime_id = seat.showtime_id AND seat.sidx DIV 2 = rv.ridx
) claimed ON claimed.si_id = si.id
SET si.status = 'BOOKED', si.reservation_id = claimed.rid;

-- 3. Tickets from assigned seats
INSERT INTO ticket (reservation_id, seat_instance_id, ticket_type, price)
SELECT si.reservation_id, si.id,
       IF(st.seat_type IN ('VIP', 'COUPLE'), 'VIP', 'NORMAL'),
       si.price
FROM seat_instance si
JOIN seat_template st ON st.id = si.seat_template_id
WHERE si.status = 'BOOKED'
  AND si.reservation_id IN (SELECT id FROM reservation WHERE reservation_code LIKE 'CPXSEED%');

-- Drop seat-less reservations to keep data clean
DELETE r FROM reservation r
LEFT JOIN ticket ti ON ti.reservation_id = r.id
WHERE r.reservation_code LIKE 'CPXSEED%' AND ti.id IS NULL;

-- 4. Reservation totals from ticket prices (no voucher in seed)
UPDATE reservation r
JOIN (
    SELECT ti.reservation_id, SUM(ti.price) AS amt
    FROM ticket ti
    WHERE ti.reservation_id IN (SELECT id FROM reservation WHERE reservation_code LIKE 'CPXSEED%')
    GROUP BY ti.reservation_id
) t ON t.reservation_id = r.id
SET r.original_amount = t.amt,
    r.total_amount    = t.amt,
    r.discount_amount = 0,
    r.is_paid         = TRUE;

-- 5. Payments per CONFIRMED reservation (only PAID carries paid_at)
INSERT INTO payment (reservation_code, payment_intent_id, gateway_txn_ref, amount,
                     method, status, paid_at, created_at)
SELECT
    x.reservation_code,
    CONCAT('seed-intent-', x.id),
    CONCAT('seed-txn-', x.id),
    x.total_amount,
    x.pmth,
    x.pst,
    CASE WHEN x.pst = 'PAID'
         THEN DATE_ADD(x.created_at, INTERVAL FLOOR(RAND() * 600) + 2 MINUTE)
         ELSE NULL END,
    x.created_at
FROM (
    SELECT r.id, r.reservation_code, r.total_amount, r.created_at,
           CASE
               WHEN MOD(r.id, 100) < 88 THEN 'PAID'
               WHEN MOD(r.id, 100) < 93 THEN 'REFUNDED'
               ELSE 'FAILED'
           END AS pst,
           ELT(1 + FLOOR(RAND() * 2), 'MOMO', 'VNPAY') AS pmth
    FROM reservation r
    WHERE r.reservation_code LIKE 'CPXSEED%' AND r.status = 'CONFIRMED'
) x;
