-- 核心能力底座分类与子项数据表

-- 1. 创建分类表
CREATE TABLE IF NOT EXISTS cms_capability_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(128) NOT NULL COMMENT '分类名称',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示，1显示，0隐藏',
    sort_order INT NOT NULL DEFAULT 99 COMMENT '排序值',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '修改人ID',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT pk_cms_capability_category PRIMARY KEY (id),
    CONSTRAINT uk_cms_cap_cat_name_deleted UNIQUE (name, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核心能力底座分类表';

CREATE INDEX idx_cms_cap_cat_sort_visible ON cms_capability_category (sort_order, visible, deleted_marker);

-- 2. 创建具体子项表
CREATE TABLE IF NOT EXISTS cms_capability_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '子项ID',
    category_id BIGINT NOT NULL COMMENT '所属分类ID',
    name VARCHAR(128) NOT NULL COMMENT '子项能力名称',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示，1显示，0隐藏',
    sort_order INT NOT NULL DEFAULT 99 COMMENT '排序值',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '修改人ID',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT pk_cms_capability_item PRIMARY KEY (id),
    CONSTRAINT uk_cms_cap_item_cat_name_deleted UNIQUE (category_id, name, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核心能力具体子项表';

CREATE INDEX idx_cms_cap_item_cat_sort_visible ON cms_capability_item (category_id, sort_order, visible, deleted_marker);

-- 预置种子底座数据
INSERT INTO cms_capability_category (id, name, visible, sort_order, version, deleted_marker) VALUES
(-9601, '企业经营管理能力', 1, 1, 0, 0),
(-9602, '全域数据智能能力', 1, 2, 0, 0),
(-9603, 'AI应用与智能体能力', 1, 3, 0, 0);

-- 预置种子具体勾选子项
INSERT INTO cms_capability_item (category_id, name, visible, sort_order, version, deleted_marker) VALUES
(-9601, 'ERP 集团管控', 1, 10, 0, 0),
(-9601, '业财一体化', 1, 20, 0, 0),
(-9601, '供应链协同', 1, 30, 0, 0),
(-9601, '智能预算管理', 1, 40, 0, 0),
(-9602, '全域数据集成', 1, 10, 0, 0),
(-9602, '智能分析预测', 1, 20, 0, 0),
(-9603, '企业知识库 (RAG)', 1, 10, 0, 0),
(-9603, '自主进化 AI Agent', 1, 20, 0, 0);
