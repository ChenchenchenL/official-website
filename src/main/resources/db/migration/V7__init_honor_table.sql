CREATE TABLE IF NOT EXISTS cms_honor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    icon_id BIGINT NOT NULL,
    visible TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_cms_honor PRIMARY KEY (id),
    CONSTRAINT uk_cms_honor_name_deleted_marker UNIQUE (name, deleted_marker)
);

CREATE INDEX idx_cms_honor_deleted_sort
    ON cms_honor (deleted_marker, sort_order, id);

CREATE INDEX idx_cms_honor_visible_deleted_sort
    ON cms_honor (visible, deleted_marker, sort_order, id);

UPDATE media_asset ma
SET status = 'BOUND'
WHERE ma.deleted_marker = 0
  AND ma.status = 'PUBLIC'
  AND EXISTS (
      SELECT 1
      FROM media_reference mr
      WHERE mr.media_id = ma.id
        AND mr.deleted_marker = 0
  );

UPDATE media_asset ma
SET status = 'TEMPORARY'
WHERE ma.deleted_marker = 0
  AND ma.status = 'PUBLIC'
  AND NOT EXISTS (
      SELECT 1
      FROM media_reference mr
      WHERE mr.media_id = ma.id
        AND mr.deleted_marker = 0
  );

INSERT INTO media_asset (
    id,
    media_type,
    status,
    original_filename,
    content_type,
    storage_path,
    public_url,
    file_size,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9101, 'IMAGE', 'BOUND', 'national-high-tech-enterprise.png', 'image/png',
       'seed/honors/national-high-tech-enterprise.png',
       '/media/public/seed/honors/national-high-tech-enterprise.png',
       68, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM media_asset WHERE id = -9101);

INSERT INTO media_asset (
    id,
    media_type,
    status,
    original_filename,
    content_type,
    storage_path,
    public_url,
    file_size,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9102, 'IMAGE', 'BOUND', 'hubei-science-innovation-enterprise.png', 'image/png',
       'seed/honors/hubei-science-innovation-enterprise.png',
       '/media/public/seed/honors/hubei-science-innovation-enterprise.png',
       68, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM media_asset WHERE id = -9102);

INSERT INTO media_asset (
    id,
    media_type,
    status,
    original_filename,
    content_type,
    storage_path,
    public_url,
    file_size,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9103, 'IMAGE', 'BOUND', 'hubei-artificial-intelligence-enterprise.png', 'image/png',
       'seed/honors/hubei-artificial-intelligence-enterprise.png',
       '/media/public/seed/honors/hubei-artificial-intelligence-enterprise.png',
       68, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM media_asset WHERE id = -9103);

INSERT INTO media_asset (
    id,
    media_type,
    status,
    original_filename,
    content_type,
    storage_path,
    public_url,
    file_size,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9104, 'IMAGE', 'BOUND', 'china-optics-valley-3551-enterprise.png', 'image/png',
       'seed/honors/china-optics-valley-3551-enterprise.png',
       '/media/public/seed/honors/china-optics-valley-3551-enterprise.png',
       68, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM media_asset WHERE id = -9104);

INSERT INTO cms_honor (
    id,
    name,
    icon_id,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9201, '国家高新技术企业', -9101, 1, 10, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_honor WHERE id = -9201);

INSERT INTO cms_honor (
    id,
    name,
    icon_id,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9202, '湖北省科技创新企业', -9102, 1, 20, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_honor WHERE id = -9202);

INSERT INTO cms_honor (
    id,
    name,
    icon_id,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9203, '湖北省人工智能企业', -9103, 1, 30, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_honor WHERE id = -9203);

INSERT INTO cms_honor (
    id,
    name,
    icon_id,
    visible,
    sort_order,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9204, '中国光谷3551企业', -9104, 1, 40, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_honor WHERE id = -9204);

INSERT INTO media_reference (
    id,
    media_id,
    biz_module,
    biz_object_id,
    biz_field,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9301, -9101, 'SITE', -9201, 'icon', 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM media_reference WHERE id = -9301);

INSERT INTO media_reference (
    id,
    media_id,
    biz_module,
    biz_object_id,
    biz_field,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9302, -9102, 'SITE', -9202, 'icon', 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM media_reference WHERE id = -9302);

INSERT INTO media_reference (
    id,
    media_id,
    biz_module,
    biz_object_id,
    biz_field,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9303, -9103, 'SITE', -9203, 'icon', 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM media_reference WHERE id = -9303);

INSERT INTO media_reference (
    id,
    media_id,
    biz_module,
    biz_object_id,
    biz_field,
    version,
    created_by,
    updated_by,
    deleted_marker
)
SELECT -9304, -9104, 'SITE', -9204, 'icon', 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM media_reference WHERE id = -9304);
