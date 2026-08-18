CREATE TABLE audit_log (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    action     VARCHAR(64)  NOT NULL,
    entity     VARCHAR(64)  NOT NULL,
    entity_id  VARCHAR(64)  NULL,
    actor_id   BIGINT       NULL,
    from_state VARCHAR(32)  NULL,
    to_state   VARCHAR(32)  NULL,
    metadata   VARCHAR(1024) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_entity (entity, entity_id),
    INDEX idx_audit_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;