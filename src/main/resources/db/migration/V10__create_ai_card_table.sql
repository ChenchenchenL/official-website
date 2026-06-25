-- AI战略模块化卡片表
CREATE TABLE IF NOT EXISTS cms_ai_card (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(128) NOT NULL COMMENT '中文主标题',
    english_name VARCHAR(128) NULL COMMENT '英文副标',
    icon_id BIGINT NULL COMMENT '卡片Icon媒体文件ID',
    description VARCHAR(256) NOT NULL COMMENT '一句话业务描述',
    jump_link VARCHAR(256) NULL COMMENT '跳转链接',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否前台显示，1显示，0隐藏',
    sort_order INT NOT NULL DEFAULT 99 COMMENT '排序值，越小越靠前',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NULL COMMENT '创建者 ID',
    updated_by BIGINT NULL COMMENT '修改者 ID',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0活跃，删除时写入主键ID',
    CONSTRAINT pk_cms_ai_card PRIMARY KEY (id),
    CONSTRAINT uk_cms_ai_card_name_deleted UNIQUE (name, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI战略模块化卡片表';

-- 前台过滤与排序复合索引
CREATE INDEX idx_cms_ai_card_sort_visible ON cms_ai_card (sort_order, visible, deleted_marker);

-- 后台管理列表过滤索引
CREATE INDEX idx_cms_ai_card_deleted_sort ON cms_ai_card (deleted_marker, sort_order, id);

-- 预置默认种子数据 (原PRD中固定的四大卡片)
INSERT INTO cms_ai_card (
    id, name, english_name, description, visible, sort_order, version, created_by, updated_by, deleted_marker
)
SELECT -9501, '企业知识库', 'Knowledge', '沉淀组织经验与业务知识，构建企业专属知识大脑', 1, 1, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_ai_card WHERE id = -9501);

INSERT INTO cms_ai_card (
    id, name, english_name, description, visible, sort_order, version, created_by, updated_by, deleted_marker
)
SELECT -9502, '智能助手', 'Assistant', '打造多场景AI助手，实现办公、协同与业务处理效率翻倍', 1, 2, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_ai_card WHERE id = -9502);

INSERT INTO cms_ai_card (
    id, name, english_name, description, visible, sort_order, version, created_by, updated_by, deleted_marker
)
SELECT -9503, '数据智能', 'Analytics', '整合全域数据资产，实现智能预测、分析与智能辅助决策', 1, 3, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_ai_card WHERE id = -9503);

INSERT INTO cms_ai_card (
    id, name, english_name, description, visible, sort_order, version, created_by, updated_by, deleted_marker
)
SELECT -9504, '企业智能体', 'Agent', '发布自主进化智能体产品，开启自主化业务流运转新阶段', 1, 4, 0, NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM cms_ai_card WHERE id = -9504);
