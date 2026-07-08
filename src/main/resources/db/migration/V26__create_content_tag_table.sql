CREATE TABLE IF NOT EXISTS content_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tag_code VARCHAR(64) NOT NULL,
    tag_name VARCHAR(64) NOT NULL,
    description VARCHAR(512) NULL,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_content_tag PRIMARY KEY (id),
    CONSTRAINT uk_content_tag_code_del UNIQUE (tag_code, deleted_marker),
    CONSTRAINT uk_content_tag_name_del UNIQUE (tag_name, deleted_marker)
);

CREATE INDEX idx_content_tag_del_visible_sort
    ON content_tag (deleted_marker, visible, sort_order, id);

INSERT INTO content_tag (
    tag_code,
    tag_name,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT 'AI', 'AI', '人工智能与智能体相关内容', 1, 10, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_tag WHERE tag_code = 'AI' AND deleted_marker = 0);

INSERT INTO content_tag (
    tag_code,
    tag_name,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT 'BIG_DATA', '大数据', '数据治理、数据分析与数据平台相关内容', 1, 20, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_tag WHERE tag_code = 'BIG_DATA' AND deleted_marker = 0);

INSERT INTO content_tag (
    tag_code,
    tag_name,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT 'INDUSTRIAL_INTERNET', '工业互联网', '工业场景、制造业和产业互联网相关内容', 1, 30, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_tag WHERE tag_code = 'INDUSTRIAL_INTERNET' AND deleted_marker = 0);

INSERT INTO content_tag (
    tag_code,
    tag_name,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT 'MEDICAL', '医疗', '医疗健康行业相关内容', 1, 40, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_tag WHERE tag_code = 'MEDICAL' AND deleted_marker = 0);

INSERT INTO content_tag (
    tag_code,
    tag_name,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT 'EDUCATION', '教育', '教育行业和智慧教育相关内容', 1, 50, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_tag WHERE tag_code = 'EDUCATION' AND deleted_marker = 0);

INSERT INTO content_tag (
    tag_code,
    tag_name,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT 'AGENT', '智能体', '智能体应用与自动化流程相关内容', 1, 60, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_tag WHERE tag_code = 'AGENT' AND deleted_marker = 0);

INSERT INTO content_tag (
    tag_code,
    tag_name,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT 'SAAS', 'SaaS', 'SaaS 平台与订阅式软件相关内容', 1, 70, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM content_tag WHERE tag_code = 'SAAS' AND deleted_marker = 0);
