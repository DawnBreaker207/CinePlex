-- Review moderation flag (new feature DDL, not a baseline rewrite).
ALTER TABLE review ADD COLUMN moderated BOOLEAN NOT NULL DEFAULT FALSE;
