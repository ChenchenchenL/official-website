CREATE TABLE IF NOT EXISTS cms_partner_university (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '高校简称',
    full_name VARCHAR(200) NOT NULL COMMENT '高校全称',
    logo_media_id BIGINT NOT NULL COMMENT '高校Logo媒体文件ID',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否前台显示，1显示，0隐藏',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建者ID',
    updated_by BIGINT NULL COMMENT '修改者ID',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0活跃，删除时写入主键ID',
    CONSTRAINT pk_cms_partner_university PRIMARY KEY (id),
    CONSTRAINT uk_cms_partner_university_name_deleted UNIQUE (name, deleted_marker),
    CONSTRAINT uk_cms_partner_university_full_name_deleted UNIQUE (full_name, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合作高校配置表';

CREATE INDEX idx_cms_partner_university_visible_deleted_sort
    ON cms_partner_university (visible, deleted_marker, sort_order, id);

CREATE INDEX idx_cms_partner_university_logo_media_id
    ON cms_partner_university (logo_media_id);

CREATE TABLE IF NOT EXISTS cms_research_direction (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title_cn VARCHAR(100) NOT NULL COMMENT '中文标题',
    title_en VARCHAR(100) NOT NULL COMMENT '英文标题',
    summary VARCHAR(512) NOT NULL COMMENT '研发方向描述/摘要',
    icon_media_id BIGINT NOT NULL COMMENT '研发方向Icon媒体文件ID',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否前台显示，1显示，0隐藏',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建者ID',
    updated_by BIGINT NULL COMMENT '修改者ID',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0活跃，删除时写入主键ID',
    CONSTRAINT pk_cms_research_direction PRIMARY KEY (id),
    CONSTRAINT uk_cms_research_direction_title_cn_deleted UNIQUE (title_cn, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='重点研发方向配置表';

CREATE INDEX idx_cms_research_direction_visible_deleted_sort
    ON cms_research_direction (visible, deleted_marker, sort_order, id);

CREATE INDEX idx_cms_research_direction_icon_media_id
    ON cms_research_direction (icon_media_id);

