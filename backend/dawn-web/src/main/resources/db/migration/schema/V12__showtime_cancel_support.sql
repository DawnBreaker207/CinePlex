-- Showtime cancellation support (new feature DDL, not a baseline rewrite).
ALTER TABLE showtime ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE payment MODIFY COLUMN status
    ENUM('PENDING','PAID','FAILED','CANCELED','REFUNDED','REFUND_PENDING') NOT NULL DEFAULT 'PENDING';
