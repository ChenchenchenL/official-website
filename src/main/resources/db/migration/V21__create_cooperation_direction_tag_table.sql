-- 创建"合作方向标签"表，承载"联系我们"页面左侧蓝色区域的可维护标签列表。
CREATE TABLE cms_cooperation_direction_tag (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    tag_text      VARCHAR(32)  NOT NULL                COMMENT '标签文本',
    sort_order    INT          NOT NULL DEFAULT 0      COMMENT '排序值（越小越靠前）',
    version       INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    created_at    DATETIME     NOT NULL                COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL                COMMENT '更新时间',
    created_by    BIGINT       NOT NULL                COMMENT '创建人 ID',
    updated_by    BIGINT       NOT NULL                COMMENT '更新人 ID',
    deleted_marker BIGINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除标记',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='合作方向标签';

-- 活跃数据中 tag_text 全局唯一，结合逻辑删除实现"删除后可同名重建"。
CREATE UNIQUE INDEX uk_cms_coop_direction_tag_text_del
    ON cms_cooperation_direction_tag (tag_text, deleted_marker);

-- 前后台列表查询路径：先过滤活跃数据，再按 sort_order + id 排序。
CREATE INDEX idx_cms_coop_direction_tag_del_sort
    ON cms_cooperation_direction_tag (deleted_marker, sort_order, id);
