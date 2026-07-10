CREATE TABLE IF NOT EXISTS business_page (
    id BIGINT NOT NULL AUTO_INCREMENT,
    business_id BIGINT NOT NULL,
    template_id BIGINT NULL,
    page_code VARCHAR(64) NOT NULL,
    page_name VARCHAR(128) NOT NULL,
    route_path VARCHAR(256) NOT NULL,
    page_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    page_config TEXT NULL,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_business_page PRIMARY KEY (id),
    CONSTRAINT uk_business_page_code_del UNIQUE (page_code, deleted_marker),
    CONSTRAINT uk_business_page_route_del UNIQUE (route_path, deleted_marker)
);

CREATE INDEX idx_business_page_business_sort
    ON business_page (deleted_marker, business_id, page_status, sort_order, id);

CREATE TABLE IF NOT EXISTS business_page_block (
    id BIGINT NOT NULL AUTO_INCREMENT,
    page_id BIGINT NOT NULL,
    block_id BIGINT NOT NULL,
    block_code VARCHAR(64) NOT NULL,
    block_name VARCHAR(128) NOT NULL,
    block_type VARCHAR(64) NOT NULL,
    block_config TEXT NULL,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_business_page_block PRIMARY KEY (id)
);

CREATE INDEX idx_business_page_block_page_sort
    ON business_page_block (deleted_marker, page_id, sort_order, id);
