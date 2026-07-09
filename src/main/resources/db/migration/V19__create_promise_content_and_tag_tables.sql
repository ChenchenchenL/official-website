CREATE TABLE IF NOT EXISTS cms_promise_content (
    id BIGINT NOT NULL AUTO_INCREMENT,
    config_key VARCHAR(64) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_cms_promise_content PRIMARY KEY (id),
    CONSTRAINT uk_cms_promise_content_key_del UNIQUE (config_key, deleted_marker)
);

CREATE TABLE IF NOT EXISTS cms_promise_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tag_text VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_cms_promise_tag PRIMARY KEY (id),
    CONSTRAINT uk_cms_promise_tag_text_del UNIQUE (tag_text, deleted_marker)
);

CREATE INDEX idx_cms_promise_tag_del_sort
    ON cms_promise_tag (deleted_marker, sort_order, id);

-- 单例主体文案默认记录
INSERT INTO cms_promise_content (
    config_key,
    content,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT
    'default',
    '云台数据全体员工将继续秉承专业、务实、创新、共赢的理念，为客户持续创造真实价值。',
    0,
    NULL,
    NULL,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM cms_promise_content WHERE config_key = 'default' AND deleted_marker = 0
);

-- 默认承诺标签
INSERT INTO cms_promise_tag (
    id,
    tag_text,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9501, '过硬的技术', 10, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_promise_tag WHERE id = -9501);

INSERT INTO cms_promise_tag (
    id,
    tag_text,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9502, '简便的操作', 20, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_promise_tag WHERE id = -9502);

INSERT INTO cms_promise_tag (
    id,
    tag_text,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9503, '实用的功能', 30, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_promise_tag WHERE id = -9503);
