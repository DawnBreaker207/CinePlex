-- Phase D: optimistic locking + outbox idempotency
ALTER TABLE reservation ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payment ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE outbox ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE outbox ADD UNIQUE INDEX ux_outbox_event_reservation (event_type, reservation_id);