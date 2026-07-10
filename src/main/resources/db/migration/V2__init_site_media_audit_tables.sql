CREATE TABLE IF NOT EXISTS cms_site_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    config_key VARCHAR(64) NOT NULL,
    site_title VARCHAR(120) NULL,
    seo_keywords VARCHAR(255) NULL,
    seo_description VARCHAR(500) NULL,
    brand_slogan VARCHAR(160) NULL,
    brand_tagline VARCHAR(255) NULL,
    logo_light_media_id BIGINT NULL,
    logo_dark_media_id BIGINT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_cms_site_config PRIMARY KEY (id),
    CONSTRAINT uk_cms_site_config_key_deleted_marker UNIQUE (config_key, deleted_marker)
);

CREATE TABLE IF NOT EXISTS media_asset (
    id BIGINT NOT NULL AUTO_INCREMENT,
    media_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    public_url VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_media_asset PRIMARY KEY (id)
);

CREATE INDEX idx_media_asset_type_status_deleted_marker
    ON media_asset (media_type, status, deleted_marker);

CREATE TABLE IF NOT EXISTS media_reference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    media_id BIGINT NOT NULL,
    biz_module VARCHAR(64) NOT NULL,
    biz_object_id BIGINT NOT NULL,
    biz_field VARCHAR(64) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_media_reference PRIMARY KEY (id),
    CONSTRAINT uk_media_reference_biz_field_deleted_marker UNIQUE (biz_module, biz_object_id, biz_field, deleted_marker)
);

CREATE INDEX idx_media_reference_media_id_deleted_marker
    ON media_reference (media_id, deleted_marker);

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    module_name VARCHAR(64) NOT NULL,
    action_name VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id BIGINT NOT NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(64) NULL,
    request_ip VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    result VARCHAR(32) NOT NULL,
    trace_id VARCHAR(64) NULL,
    occurred_at DATETIME NOT NULL,
    before_snapshot TEXT NULL,
    after_snapshot TEXT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_audit_log PRIMARY KEY (id)
);

CREATE INDEX idx_sys_audit_log_module_action_occurred_at
    ON sys_audit_log (module_name, action_name, occurred_at);

INSERT INTO cms_site_config (
    config_key,
    site_title,
    seo_keywords,
    seo_description,
    brand_slogan,
    brand_tagline,
    logo_light_media_id,
    logo_dark_media_id,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT
    'default',
    '',
    '',
    '',
    '',
    '',
    NULL,
    NULL,
    0,
    NULL,
    NULL,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM cms_site_config WHERE config_key = 'default' AND deleted_marker = 0
);
