CREATE TABLE IF NOT EXISTS cms_industry_solution (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '行业解决方案名称',
    icon_media_id BIGINT NOT NULL COMMENT '行业图标媒体文件ID',
    description VARCHAR(500) NOT NULL COMMENT '行业方案描述',
    customer_tags VARCHAR(1000) NULL COMMENT '典型客户标签(JSON数组)',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否前台显示，1显示，0隐藏',
    sort_order INT NOT NULL DEFAULT 10 COMMENT '排序值，越小越靠前',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建者 ID',
    updated_by BIGINT NULL COMMENT '修改者 ID',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT pk_cms_industry_solution PRIMARY KEY (id),
    CONSTRAINT uk_cms_industry_solution_name_deleted UNIQUE (name, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行业解决方案配置表';

CREATE INDEX idx_cms_industry_solution_visible_del ON cms_industry_solution (visible, deleted_marker, sort_order, id);
CREATE INDEX idx_cms_industry_solution_sort_visible ON cms_industry_solution (sort_order, visible, deleted_marker);
CREATE INDEX idx_cms_industry_solution_icon_id ON cms_industry_solution (icon_media_id);
