-- Ticket check-in (single-use entry verification at the box office).
-- New-feature DDL, not a baseline rewrite.
ALTER TABLE reservation
    ADD COLUMN checked_in BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN checked_in_at DATETIME NULL;
