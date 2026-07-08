CREATE TABLE IF NOT EXISTS business_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    template_type VARCHAR(64) NOT NULL,
    default_business_code VARCHAR(64) NULL,
    default_business_name VARCHAR(128) NULL,
    default_icon_media_id BIGINT NULL,
    default_business_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    description VARCHAR(512) NULL,
    template_config TEXT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_business_template PRIMARY KEY (id),
    CONSTRAINT uk_business_template_code_del UNIQUE (template_code, deleted_marker)
);

CREATE INDEX idx_business_template_type_sort
    ON business_template (deleted_marker, template_type, sort_order, id);
