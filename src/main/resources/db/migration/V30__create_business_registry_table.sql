CREATE TABLE IF NOT EXISTS business_registry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    business_code VARCHAR(64) NOT NULL,
    business_name VARCHAR(128) NOT NULL,
    icon_media_id BIGINT NULL,
    description VARCHAR(512) NULL,
    business_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_business_registry PRIMARY KEY (id),
    CONSTRAINT uk_business_registry_code_del UNIQUE (business_code, deleted_marker)
);

CREATE INDEX idx_business_registry_status_sort
    ON business_registry (deleted_marker, business_status, sort_order, id);
