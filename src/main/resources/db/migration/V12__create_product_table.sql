CREATE TABLE IF NOT EXISTS cms_product (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '产品ID',
    name VARCHAR(128) NOT NULL COMMENT '产品名称',
    logo_id BIGINT NOT NULL COMMENT '产品Logo媒体文件ID',
    sub_title VARCHAR(256) NULL COMMENT '副标题',
    abstract_text VARCHAR(512) NOT NULL COMMENT '产品摘要',
    status_tag VARCHAR(64) NULL COMMENT '状态标签(如已发布、即将发布)',
    detail_link VARCHAR(256) NULL COMMENT '详情跳转链接',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否上架显示，1显示，0隐藏',
    sort_order INT NOT NULL DEFAULT 99 COMMENT '排序值，越小越靠前',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建者 ID',
    updated_by BIGINT NULL COMMENT '修改者 ID',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT pk_cms_product PRIMARY KEY (id),
    CONSTRAINT uk_cms_product_name_deleted UNIQUE (name, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品矩阵配置表';

CREATE INDEX idx_cms_product_sort_visible ON cms_product (sort_order, visible, deleted_marker);
