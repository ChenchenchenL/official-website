-- 企业实力核心指标表
CREATE TABLE IF NOT EXISTS cms_strength_metric (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    icon_id BIGINT NULL COMMENT '指标图标媒体文件ID，允许为空',
    metric_value VARCHAR(64) NOT NULL COMMENT '核心数值，如 50+、100%、5大领域',
    label VARCHAR(128) NOT NULL COMMENT '业务标签，如 服务客户、项目交付',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否前台显示，1显示，0隐藏',
    sort_order INT NOT NULL DEFAULT 99 COMMENT '排序值，越小越靠前',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建者ID',
    updated_by BIGINT NULL COMMENT '修改者ID',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0活跃，删除时写入主键ID',
    CONSTRAINT pk_cms_strength_metric PRIMARY KEY (id),
    CONSTRAINT uk_cms_strength_metric_label_deleted UNIQUE (label, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业实力核心指标表';

-- 前台过滤与排序复合索引
CREATE INDEX idx_cms_strength_metric_sort_visible
    ON cms_strength_metric (sort_order, visible, deleted_marker);

-- 后台管理列表过滤索引
CREATE INDEX idx_cms_strength_metric_deleted_sort
    ON cms_strength_metric (deleted_marker, sort_order, id);

-- 预置默认种子数据（icon_id 为空，种子指标不依赖媒体资源）
INSERT INTO cms_strength_metric (
    id, metric_value, label, visible, sort_order, version, created_by, updated_by, deleted_marker
)
SELECT -9401, '50+', '服务客户', 1, 10, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_strength_metric WHERE id = -9401);

INSERT INTO cms_strength_metric (
    id, metric_value, label, visible, sort_order, version, created_by, updated_by, deleted_marker
)
SELECT -9402, '5大领域', '行业覆盖', 1, 20, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_strength_metric WHERE id = -9402);

INSERT INTO cms_strength_metric (
    id, metric_value, label, visible, sort_order, version, created_by, updated_by, deleted_marker
)
SELECT -9403, '100%', '项目交付', 1, 30, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_strength_metric WHERE id = -9403);
