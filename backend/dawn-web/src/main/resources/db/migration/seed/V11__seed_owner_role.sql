-- Owner role for the RBAC owner model (system owner outranks admin).
-- INSERT IGNORE: safe to re-run, never duplicates.
INSERT IGNORE INTO roles (name) VALUES ('OWNER');
