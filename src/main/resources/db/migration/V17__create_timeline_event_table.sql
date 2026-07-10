CREATE TABLE IF NOT EXISTS cms_timeline_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_year INT NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(512) NOT NULL,
    visible TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_cms_timeline_event PRIMARY KEY (id),
    CONSTRAINT uk_cms_timeline_year_title_del UNIQUE (event_year, title, deleted_marker)
);

CREATE INDEX idx_cms_timeline_visible_del_year
    ON cms_timeline_event (visible, deleted_marker, event_year, sort_order, id);

INSERT INTO cms_timeline_event (
    id,
    event_year,
    title,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9301, 2019, '公司成立', '公司正式注册成立，开启数字化征程。', 1, 10, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_timeline_event WHERE id = -9301);

INSERT INTO cms_timeline_event (
    id,
    event_year,
    title,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9302, 2020, '核心产品发布', '发布首款企业级智能数据平台。', 1, 20, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_timeline_event WHERE id = -9302);

INSERT INTO cms_timeline_event (
    id,
    event_year,
    title,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9303, 2021, '获得战略融资', '完成 A 轮融资，加速产品研发与市场拓展。', 1, 30, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_timeline_event WHERE id = -9303);

INSERT INTO cms_timeline_event (
    id,
    event_year,
    title,
    description,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9304, 2022, '全国市场拓展', '业务覆盖全国主要城市，客户数量突破千家。', 1, 40, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_timeline_event WHERE id = -9304);
