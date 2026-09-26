-- Payment seat snapshot for post-payment reconciliation (C4):
-- seats changed after payment must not be confirmed silently.
-- New-feature DDL, not a baseline rewrite.
ALTER TABLE payment
    ADD COLUMN seat_snapshot TEXT NULL;
