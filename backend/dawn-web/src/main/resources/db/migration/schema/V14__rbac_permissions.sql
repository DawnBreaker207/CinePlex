-- RBAC permission matrix (deny-by-default) + voucher maker-checker columns.
-- New-feature DDL, not a baseline rewrite.
CREATE TABLE admin_permissions (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id        BIGINT NOT NULL,
    permission_key VARCHAR(20) NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_perm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_admin_perm_user_key (user_id, permission_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

ALTER TABLE vouchers ADD COLUMN created_by_admin_id BIGINT NULL,
    ADD CONSTRAINT fk_voucher_creator FOREIGN KEY (created_by_admin_id)
    REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE vouchers MODIFY COLUMN status
    ENUM('SCHEDULED','ACTIVE','PAUSED','EXPIRED','EXHAUSTED','PENDING_APPROVAL')
    NOT NULL DEFAULT 'SCHEDULED';
