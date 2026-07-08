CREATE TABLE IF NOT EXISTS page_section (
    id BIGINT NOT NULL AUTO_INCREMENT,
    page_code VARCHAR(64) NOT NULL,
    section_code VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    subtitle VARCHAR(255) NULL,
    description VARCHAR(1000) NULL,
    content_json TEXT NULL,
    sort_order INT NOT NULL DEFAULT 99,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_page_section PRIMARY KEY (id),
    CONSTRAINT uk_page_section_page_code_section_deleted UNIQUE (page_code, section_code, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='页面区块配置表';

CREATE INDEX idx_page_section_page_visible_status_sort
    ON page_section (page_code, visible, status, deleted_marker, sort_order, id);

CREATE INDEX idx_page_section_deleted_sort
    ON page_section (deleted_marker, sort_order, id);
