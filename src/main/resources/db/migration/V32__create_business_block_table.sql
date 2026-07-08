CREATE TABLE IF NOT EXISTS business_block (
    id BIGINT NOT NULL AUTO_INCREMENT,
    block_code VARCHAR(64) NOT NULL,
    block_name VARCHAR(128) NOT NULL,
    block_type VARCHAR(64) NOT NULL,
    description VARCHAR(512) NULL,
    default_config TEXT NULL,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_business_block PRIMARY KEY (id),
    CONSTRAINT uk_business_block_code_del UNIQUE (block_code, deleted_marker)
);

CREATE INDEX idx_business_block_type_sort
    ON business_block (deleted_marker, block_type, visible, sort_order, id);
