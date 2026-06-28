CREATE TABLE IF NOT EXISTS cms_contact_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    config_key VARCHAR(32) NOT NULL,
    contact_address VARCHAR(255) NOT NULL,
    business_phone VARCHAR(64) NOT NULL,
    contact_email VARCHAR(128) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_cms_contact_info PRIMARY KEY (id),
    CONSTRAINT uk_cms_contact_info_key_del UNIQUE (config_key, deleted_marker)
);

-- 单例基础联系方式默认记录
INSERT INTO cms_contact_info (
    config_key,
    contact_address,
    business_phone,
    contact_email,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT
    'default',
    '武汉市东湖新技术开发区光谷大道77号',
    '+86 027-88886666',
    'business@example.com',
    0,
    NULL,
    NULL,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM cms_contact_info WHERE config_key = 'default' AND deleted_marker = 0
);
