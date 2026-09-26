-- Store token hash + keep revoked rows (not hard-deleted) for reuse detection.
ALTER TABLE refresh_token
    CHANGE COLUMN token token_hash VARCHAR(512) NOT NULL,
    ADD COLUMN revoked_at DATETIME NULL;
