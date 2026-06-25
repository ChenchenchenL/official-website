CREATE TABLE IF NOT EXISTS cms_client_logo (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(128) NOT NULL COMMENT '客户名称',
    industry VARCHAR(64) NULL COMMENT '所属行业',
    logo_id BIGINT NOT NULL COMMENT '客户Logo媒体文件ID',
    visible TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否前台显示，1显示，0隐藏',
    sort_order INT NOT NULL DEFAULT 99 COMMENT '排序值，越小越靠前',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建者 ID',
    updated_by BIGINT NULL COMMENT '修改者 ID',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT pk_cms_client_logo PRIMARY KEY (id),
    CONSTRAINT uk_cms_client_logo_name_deleted UNIQUE (name, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务客户Logo墙表';

CREATE INDEX idx_cms_client_logo_deleted_sort
    ON cms_client_logo (deleted_marker, sort_order, id);

CREATE INDEX idx_cms_client_logo_visible_deleted_sort
    ON cms_client_logo (visible, deleted_marker, sort_order, id);
