CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_login_at DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_user PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_username_deleted_marker UNIQUE (username, deleted_marker)
);

CREATE INDEX IF NOT EXISTS idx_sys_user_role_code_deleted_marker ON sys_user (role_code, deleted_marker);
CREATE INDEX IF NOT EXISTS idx_sys_user_status_deleted_marker ON sys_user (status, deleted_marker);

INSERT INTO sys_user (
    username,
    password_hash,
    display_name,
    role_code,
    status,
    last_login_at,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT
    'admin',
    '$2a$10$FIrWp7hmOOezCwXWiaurve0KNTc6pRBSWjIAff0HgXOTUfDxuX9NK',
    '系统管理员',
    'ADMINISTRATOR',
    'ENABLED',
    NULL,
    0,
    NULL,
    NULL,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user WHERE username = 'admin' AND deleted_marker = 0
);
