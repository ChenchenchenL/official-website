CREATE TABLE IF NOT EXISTS cms_value_card (
    id BIGINT NOT NULL AUTO_INCREMENT,
    icon_media_id BIGINT NOT NULL,
    title VARCHAR(32) NOT NULL,
    subtitle VARCHAR(128) NOT NULL,
    description VARCHAR(512) NOT NULL,
    visible TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_cms_value_card PRIMARY KEY (id),
    CONSTRAINT uk_cms_value_card_title_del UNIQUE (title, deleted_marker)
);

CREATE INDEX IF NOT EXISTS idx_cms_value_card_visible_del_sort
    ON cms_value_card (visible, deleted_marker, sort_order, id);

INSERT INTO cms_value_card (
    id,
    icon_media_id,
    title,
    subtitle,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9401, -1, '同事', '相知相惜，并肩精进', '强调团队之间的相互理解、协作与共同成长，在并肩奋斗中彼此成就。', 1, 10, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_value_card WHERE id = -9401);

INSERT INTO cms_value_card (
    id,
    icon_media_id,
    title,
    subtitle,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9402, -1, '同仁', '彼此尊重，共赴使命', '强调共同价值取向与长期责任共担，以使命感驱动每一次协作。', 1, 20, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_value_card WHERE id = -9402);

INSERT INTO cms_value_card (
    id,
    icon_media_id,
    title,
    subtitle,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9403, -1, '同享', '共创价值，共享成果', '倡导成果共享与价值共赢，让每一份付出都得到应有的回报。', 1, 30, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_value_card WHERE id = -9403);
