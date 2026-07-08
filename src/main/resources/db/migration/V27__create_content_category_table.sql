CREATE TABLE IF NOT EXISTS content_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_id BIGINT NULL,
    category_code VARCHAR(64) NOT NULL,
    category_name VARCHAR(64) NOT NULL,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_content_category PRIMARY KEY (id),
    CONSTRAINT uk_content_category_code_del UNIQUE (category_code, deleted_marker)
);

CREATE INDEX idx_content_category_del_parent_sort
    ON content_category (deleted_marker, parent_id, sort_order, id);

CREATE INDEX idx_content_category_del_visible_sort
    ON content_category (deleted_marker, visible, sort_order, id);

INSERT INTO content_category (
    id,
    parent_id,
    category_code,
    category_name,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9701, NULL, 'AI_CAPABILITY', 'AI能力', 1, 10, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_category WHERE category_code = 'AI_CAPABILITY' AND deleted_marker = 0);

INSERT INTO content_category (
    id,
    parent_id,
    category_code,
    category_name,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9702, -9701, 'AI_ASSISTANT', '智能助手', 1, 10, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_category WHERE category_code = 'AI_ASSISTANT' AND deleted_marker = 0);

INSERT INTO content_category (
    id,
    parent_id,
    category_code,
    category_name,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9703, -9701, 'DATA_ANALYSIS', '数据分析', 1, 20, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_category WHERE category_code = 'DATA_ANALYSIS' AND deleted_marker = 0);

INSERT INTO content_category (
    id,
    parent_id,
    category_code,
    category_name,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9704, -9701, 'KNOWLEDGE_BASE', '知识库', 1, 30, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_category WHERE category_code = 'KNOWLEDGE_BASE' AND deleted_marker = 0);

INSERT INTO content_category (
    id,
    parent_id,
    category_code,
    category_name,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9711, NULL, 'INDUSTRY_SOLUTION', '行业方案', 1, 20, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_category WHERE category_code = 'INDUSTRY_SOLUTION' AND deleted_marker = 0);

INSERT INTO content_category (
    id,
    parent_id,
    category_code,
    category_name,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9712, -9711, 'MEDICAL', '医疗', 1, 10, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_category WHERE category_code = 'MEDICAL' AND deleted_marker = 0);

INSERT INTO content_category (
    id,
    parent_id,
    category_code,
    category_name,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9713, -9711, 'EDUCATION', '教育', 1, 20, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_category WHERE category_code = 'EDUCATION' AND deleted_marker = 0);

INSERT INTO content_category (
    id,
    parent_id,
    category_code,
    category_name,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9714, -9711, 'INDUSTRY', '工业', 1, 30, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_category WHERE category_code = 'INDUSTRY' AND deleted_marker = 0);
