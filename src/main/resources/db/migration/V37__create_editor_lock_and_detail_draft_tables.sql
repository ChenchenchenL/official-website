-- V37: 创建官网可视化编辑器独占锁表及产品/案例/行业方案详情草稿与版本快照表

-- 1. 独占编辑锁表
CREATE TABLE IF NOT EXISTS cms_editor_lock (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    resource_type VARCHAR(32) NOT NULL COMMENT '资源类型: PAGE, PRODUCT, CASE, INDUSTRY_SOLUTION',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    locked_by VARCHAR(64) NOT NULL COMMENT '获锁管理员账号/ID',
    owner_display_name VARCHAR(64) NOT NULL COMMENT '脱敏展示名',
    lock_token_hash VARCHAR(64) NOT NULL COMMENT 'Lock Token的SHA-256哈希摘要',
    acquired_at DATETIME NOT NULL COMMENT '获锁时间',
    last_heartbeat_at DATETIME NOT NULL COMMENT '最后心跳时间',
    expires_at DATETIME NOT NULL COMMENT '锁到期时间',
    editor_session_remark VARCHAR(255) NULL COMMENT '编辑会话备注',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记(0表示活跃锁)',
    CONSTRAINT pk_cms_editor_lock PRIMARY KEY (id),
    CONSTRAINT uk_cms_editor_lock_resource UNIQUE (resource_type, resource_id, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='官网可视化编辑器独占编辑锁表';

CREATE INDEX idx_cms_editor_lock_expire ON cms_editor_lock (deleted_marker, expires_at);

-- 2. 产品详情草稿表
CREATE TABLE IF NOT EXISTS cms_product_draft (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '关联产品ID',
    draft_json LONGTEXT NOT NULL COMMENT '详情草稿完整JSON',
    draft_hash VARCHAR(64) NOT NULL COMMENT '草稿JSON的SHA-256哈希',
    editor_session_remark VARCHAR(255) NULL COMMENT '编辑会话备注',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_by VARCHAR(64) NULL COMMENT '创建者',
    updated_by VARCHAR(64) NULL COMMENT '更新者',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT pk_cms_product_draft PRIMARY KEY (id),
    CONSTRAINT uk_cms_product_draft_pid UNIQUE (product_id, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品详情草稿表';

-- 3. 产品发布版本表
CREATE TABLE IF NOT EXISTS cms_product_version (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '关联产品ID',
    version_no INT NOT NULL COMMENT '发布版本序号',
    snapshot_json LONGTEXT NOT NULL COMMENT '发布快照完整JSON',
    snapshot_hash VARCHAR(64) NOT NULL COMMENT '快照哈希',
    change_summary VARCHAR(255) NULL COMMENT '变更说明',
    publisher VARCHAR(64) NOT NULL COMMENT '发布人',
    rollback_source_version_id BIGINT NULL COMMENT '回滚来源版本ID',
    published_at DATETIME NOT NULL COMMENT '发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT pk_cms_product_version PRIMARY KEY (id),
    CONSTRAINT uk_cms_product_version_no UNIQUE (product_id, version_no, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品发布版本历史表';

-- 4. 案例详情草稿表
CREATE TABLE IF NOT EXISTS cms_case_draft (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    case_id BIGINT NOT NULL COMMENT '关联案例ID',
    draft_json LONGTEXT NOT NULL COMMENT '详情草稿完整JSON',
    draft_hash VARCHAR(64) NOT NULL COMMENT '草稿JSON哈希',
    editor_session_remark VARCHAR(255) NULL COMMENT '编辑备注',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_by VARCHAR(64) NULL COMMENT '创建者',
    updated_by VARCHAR(64) NULL COMMENT '更新者',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT pk_cms_case_draft PRIMARY KEY (id),
    CONSTRAINT uk_cms_case_draft_cid UNIQUE (case_id, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='案例详情草稿表';

-- 5. 案例发布版本表
CREATE TABLE IF NOT EXISTS cms_case_version (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    case_id BIGINT NOT NULL COMMENT '关联案例ID',
    version_no INT NOT NULL COMMENT '发布版本序号',
    snapshot_json LONGTEXT NOT NULL COMMENT '发布快照完整JSON',
    snapshot_hash VARCHAR(64) NOT NULL COMMENT '快照哈希',
    change_summary VARCHAR(255) NULL COMMENT '变更说明',
    publisher VARCHAR(64) NOT NULL COMMENT '发布人',
    rollback_source_version_id BIGINT NULL COMMENT '回滚来源版本ID',
    published_at DATETIME NOT NULL COMMENT '发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT pk_cms_case_version PRIMARY KEY (id),
    CONSTRAINT uk_cms_case_version_no UNIQUE (case_id, version_no, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='案例发布版本历史表';

-- 6. 行业方案详情草稿表
CREATE TABLE IF NOT EXISTS cms_industry_solution_draft (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    solution_id BIGINT NOT NULL COMMENT '关联行业方案ID',
    draft_json LONGTEXT NOT NULL COMMENT '详情草稿完整JSON',
    draft_hash VARCHAR(64) NOT NULL COMMENT '草稿JSON哈希',
    editor_session_remark VARCHAR(255) NULL COMMENT '编辑备注',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_by VARCHAR(64) NULL COMMENT '创建者',
    updated_by VARCHAR(64) NULL COMMENT '更新者',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT pk_cms_industry_solution_draft PRIMARY KEY (id),
    CONSTRAINT uk_cms_industry_solution_draft_sid UNIQUE (solution_id, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行业方案详情草稿表';

-- 7. 行业方案发布版本表
CREATE TABLE IF NOT EXISTS cms_industry_solution_version (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    solution_id BIGINT NOT NULL COMMENT '关联行业方案ID',
    version_no INT NOT NULL COMMENT '发布版本序号',
    snapshot_json LONGTEXT NOT NULL COMMENT '发布快照完整JSON',
    snapshot_hash VARCHAR(64) NOT NULL COMMENT '快照哈希',
    change_summary VARCHAR(255) NULL COMMENT '变更说明',
    publisher VARCHAR(64) NOT NULL COMMENT '发布人',
    rollback_source_version_id BIGINT NULL COMMENT '回滚来源版本ID',
    published_at DATETIME NOT NULL COMMENT '发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted_marker BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    CONSTRAINT pk_cms_industry_solution_version PRIMARY KEY (id),
    CONSTRAINT uk_cms_industry_solution_version_no UNIQUE (solution_id, version_no, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行业方案发布版本历史表';
