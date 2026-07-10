-- V23__create_pagebuilder_tables.sql
-- Description: 创建页面构建器(Page Builder)相关的核心物理表，支持页面定义、草稿设计、版本历史、发布快照、组件模板以及关联数据依赖。

-- 1. 页面定义表 (cms_page_definition)
-- 承载页面的路由、元数据信息、类型与启用状态。
CREATE TABLE IF NOT EXISTS cms_page_definition (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    page_key       VARCHAR(64)  NOT NULL                COMMENT '页面唯一Key标识',
    name           VARCHAR(128) NOT NULL                COMMENT '页面名称',
    route_path     VARCHAR(255) NOT NULL                COMMENT '页面访问路由路径',
    page_type      VARCHAR(64)  NOT NULL                COMMENT '页面类型：NORMAL-普通页面, SYSTEM-系统内置页面',
    status         VARCHAR(64)  NOT NULL                COMMENT '页面状态：ENABLED-启用, DISABLED-禁用',
    visible        TINYINT      NOT NULL DEFAULT 1      COMMENT '是否前台可见，1-显示，0-隐藏',
    sort_order     INT          NOT NULL DEFAULT 0      COMMENT '排序值，越小越靠前',
    version        INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by     BIGINT       NULL DEFAULT NULL       COMMENT '创建者ID',
    updated_by     BIGINT       NULL DEFAULT NULL       COMMENT '更新者ID',
    deleted_marker BIGINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除标记，0活跃，删除时更新为ID',
    CONSTRAINT pk_cms_page_definition PRIMARY KEY (id),
    CONSTRAINT uk_cms_page_def_key_deleted UNIQUE (page_key, deleted_marker),
    CONSTRAINT uk_cms_page_def_route_deleted UNIQUE (route_path, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='页面定义表';

-- 2. 页面草稿表 (cms_page_draft)
-- 承载页面设计器的当前编辑中状态/草稿，用于暂存设计，不直接影响前台展示。
CREATE TABLE IF NOT EXISTS cms_page_draft (
    id                    BIGINT     NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    page_id               BIGINT     NOT NULL                COMMENT '页面定义ID',
    schema_json           LONGTEXT   NULL                    COMMENT '页面Schema配置JSON数据',
    schema_hash           VARCHAR(64) NULL                   COMMENT '页面Schema哈希值',
    editor_session_remark VARCHAR(255) NULL                  COMMENT '编辑会话备注/说明',
    version               INT        NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    created_at            DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at            DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by            BIGINT     NULL DEFAULT NULL       COMMENT '创建者ID',
    updated_by            BIGINT     NULL DEFAULT NULL       COMMENT '更新者ID',
    deleted_marker        BIGINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除标记，0活跃，删除时更新为ID',
    CONSTRAINT pk_cms_page_draft PRIMARY KEY (id),
    CONSTRAINT uk_cms_page_draft_page_deleted UNIQUE (page_id, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='页面草稿表';

-- 3. 页面版本历史表 (cms_page_version)
-- 承载页面设计的备份与发布版本历史。
CREATE TABLE IF NOT EXISTS cms_page_version (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    page_id        BIGINT       NOT NULL                COMMENT '页面定义ID',
    version_no     INT          NOT NULL                COMMENT '版本序号',
    source_type    VARCHAR(64)  NOT NULL                COMMENT '版本来源类型：MANUAL_SAVE-手动保存, PUBLISH_BASE-发布备份, ROLLBACK_BASE-回滚备份',
    schema_json    LONGTEXT     NULL                    COMMENT '页面Schema配置JSON数据',
    schema_hash    VARCHAR(64)  NULL                    COMMENT '页面Schema哈希值',
    change_summary VARCHAR(255) NULL                    COMMENT '版本变更描述/摘要',
    version        INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by     BIGINT       NULL DEFAULT NULL       COMMENT '创建者ID',
    updated_by     BIGINT       NULL DEFAULT NULL       COMMENT '更新者ID',
    deleted_marker BIGINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除标记，0活跃，删除时更新为ID',
    CONSTRAINT pk_cms_page_version PRIMARY KEY (id),
    CONSTRAINT uk_cms_page_ver_page_no_deleted UNIQUE (page_id, version_no, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='页面版本历史表';

-- 4. 页面发布快照表 (cms_page_publish_snapshot)
-- 记录页面发布时的最终渲染树与状态，用于前台直接加载。
CREATE TABLE IF NOT EXISTS cms_page_publish_snapshot (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    page_id        BIGINT       NOT NULL                COMMENT '页面定义ID',
    version_id     BIGINT       NOT NULL                COMMENT '关联的页面版本ID',
    snapshot_json  LONGTEXT     NULL                    COMMENT '发布快照JSON数据',
    snapshot_hash  VARCHAR(64)  NULL                    COMMENT '快照哈希值',
    publish_status VARCHAR(64)  NOT NULL                COMMENT '发布状态：ACTIVE-生效中, SUPERSEDED-被覆盖',
    active_status  VARCHAR(10)  GENERATED ALWAYS AS (CASE WHEN publish_status = 'ACTIVE' THEN 'ACTIVE' ELSE NULL END) COMMENT '用于实现单页面仅限一个ACTIVE快照的虚拟列',
    version        INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by     BIGINT       NULL DEFAULT NULL       COMMENT '创建者ID',
    updated_by     BIGINT       NULL DEFAULT NULL       COMMENT '更新者ID',
    deleted_marker BIGINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除标记，0活跃，删除时更新为ID',
    CONSTRAINT pk_cms_page_publish_snapshot PRIMARY KEY (id),
    CONSTRAINT uk_cms_page_pub_snap_active UNIQUE (page_id, active_status, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='页面发布快照表';

-- 前台页面渲染与访问的最常用索引：按页面和发布状态、创建时间查询。
CREATE INDEX idx_cms_page_pub_snap_page_status_created 
    ON cms_page_publish_snapshot (page_id, publish_status, created_at);

-- 5. 组件模板表 (cms_component_template)
-- 承载可视化构建器注册的物料/组件定义。
CREATE TABLE IF NOT EXISTS cms_component_template (
    id                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    component_code          VARCHAR(64)  NOT NULL                COMMENT '组件唯一编码',
    name                    VARCHAR(128) NOT NULL                COMMENT '组件名称',
    category                VARCHAR(64)  NOT NULL                COMMENT '组件分类',
    schema_definition_json  LONGTEXT     NULL                    COMMENT '属性Schema定义JSON',
    default_props_json      LONGTEXT     NULL                    COMMENT '默认属性Props JSON',
    binding_capability_json LONGTEXT     NULL                    COMMENT '数据绑定能力JSON',
    status                  VARCHAR(64)  NOT NULL                COMMENT '组件状态：ACTIVE-活动, INACTIVE-非活动',
    sort_order              INT          NOT NULL DEFAULT 0      COMMENT '排序值，越小越靠前',
    version                 INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by              BIGINT       NULL DEFAULT NULL       COMMENT '创建者ID',
    updated_by              BIGINT       NULL DEFAULT NULL       COMMENT '更新者ID',
    deleted_marker          BIGINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除标记，0活跃，删除时更新为ID',
    CONSTRAINT pk_cms_component_template PRIMARY KEY (id),
    CONSTRAINT uk_cms_comp_tmpl_code_deleted UNIQUE (component_code, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组件模板表';

-- 6. 页面数据依赖表 (cms_page_dependency)
-- 承载页面内容对于媒体资产或业务实体数据的显式引用关系，辅助执行一致性完整校验及引用统计。
CREATE TABLE IF NOT EXISTS cms_page_dependency (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    page_id               BIGINT       NOT NULL                COMMENT '页面定义ID',
    snapshot_id           BIGINT       NOT NULL                COMMENT '发布快照ID',
    component_instance_id VARCHAR(64)  NOT NULL                COMMENT '页面内组件实例唯一ID',
    dependency_type       VARCHAR(64)  NOT NULL                COMMENT '依赖类型：MEDIA-媒体, ENTITY-业务实体',
    target_module         VARCHAR(64)  NOT NULL                COMMENT '目标模块名称',
    target_entity_type    VARCHAR(64)  NOT NULL                COMMENT '目标实体类型',
    target_entity_id      VARCHAR(64)  NOT NULL                COMMENT '目标实体唯一标识',
    version               INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by            BIGINT       NULL DEFAULT NULL       COMMENT '创建者ID',
    updated_by            BIGINT       NULL DEFAULT NULL       COMMENT '更新者ID',
    deleted_marker        BIGINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除标记，0活跃，删除时更新为ID',
    CONSTRAINT pk_cms_page_dependency PRIMARY KEY (id),
    CONSTRAINT uk_cms_page_dep_unique UNIQUE (snapshot_id, component_instance_id, dependency_type, target_module, target_entity_type, target_entity_id, deleted_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='页面数据依赖表';

-- 查询快照下的所有组件依赖
CREATE INDEX idx_cms_page_dep_page_snapshot 
    ON cms_page_dependency (page_id, snapshot_id);

-- 反向级联删除或引用计数校验索引
CREATE INDEX idx_cms_page_dep_target 
    ON cms_page_dependency (target_module, target_entity_type, target_entity_id);
