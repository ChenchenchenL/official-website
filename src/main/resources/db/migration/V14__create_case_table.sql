CREATE TABLE IF NOT EXISTS cms_case (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(128) NOT NULL COMMENT '项目标题',
    logo_media_id BIGINT NOT NULL COMMENT '案例封面/Logo媒体文件ID',
    summary VARCHAR(512) NOT NULL COMMENT '成效摘要',
    keywords VARCHAR(1000) NULL COMMENT '核心关键词标签(JSON数组)',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否前台显示，1显示，0隐藏',
    sort_order INT NOT NULL DEFAULT 10 COMMENT '排序值，越小越靠前',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建者 ID',
    updated_by BIGINT NULL COMMENT '修改者 ID',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT pk_cms_case PRIMARY KEY (id),
    CONSTRAINT uk_cms_case_title_deleted UNIQUE (title, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标杆案例卡片配置表';

CREATE INDEX idx_cms_case_visible_del ON cms_case (visible, deleted_marker, sort_order, id);
CREATE INDEX idx_cms_case_sort_visible ON cms_case (sort_order, visible, deleted_marker);
CREATE INDEX idx_cms_case_logo_id ON cms_case (logo_media_id);
