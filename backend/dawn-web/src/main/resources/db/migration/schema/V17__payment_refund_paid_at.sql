-- Refund states keep their paid_at: V12 added REFUND_PENDING to the status
-- enum but ck_payment_paid_at still forced paid_at IS NULL for every
-- non-PAID row, so every markRefundPending/approveRefund died at the DB.
-- The check now only enforces the load-bearing direction: unpaid states
-- must not carry a paid_at. Paid states are unconstrained so legacy
-- REFUNDED seed rows (paid_at NULL) keep passing.
-- The old check is dropped conditionally: databases that ran the first
-- (failed) V17 attempt already lack it.
-- New-feature DDL, not a baseline rewrite.
DELIMITER $$
DROP PROCEDURE IF EXISTS cineplex_drop_paid_check$$
CREATE PROCEDURE cineplex_drop_paid_check()
BEGIN
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
               WHERE CONSTRAINT_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'payment'
                 AND CONSTRAINT_NAME = 'ck_payment_paid_at'
                 AND CONSTRAINT_TYPE = 'CHECK') THEN
        ALTER TABLE payment DROP CHECK ck_payment_paid_at;
    END IF;
END$$
DELIMITER ;
CALL cineplex_drop_paid_check();
DROP PROCEDURE cineplex_drop_paid_check;
ALTER TABLE payment ADD CONSTRAINT ck_payment_paid_at CHECK (
    (status IN ('PENDING', 'FAILED', 'CANCELED') AND paid_at IS NULL)
    OR status NOT IN ('PENDING', 'FAILED', 'CANCELED')
);
