-- 创建"线索记录"表，承载"预约交流"表单提交的访客留资数据。
CREATE TABLE IF NOT EXISTS cms_lead_record (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    name                VARCHAR(64)   NOT NULL                COMMENT '姓名',
    company             VARCHAR(128)  NOT NULL                COMMENT '公司',
    email               VARCHAR(128)  NOT NULL                COMMENT '邮箱',
    phone               VARCHAR(64)   NULL     DEFAULT NULL   COMMENT '电话',
    demand_description  VARCHAR(1000) NULL     DEFAULT NULL   COMMENT '需求描述',
    status              TINYINT       NOT NULL DEFAULT 0      COMMENT '跟进状态：0-未处理 1-处理中 2-已归档 3-无效线索',
    submit_ip           VARCHAR(45)   NOT NULL                COMMENT '提交 IP',
    user_agent          VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '提交 UA',
    status_updated_at   DATETIME      NULL     DEFAULT NULL   COMMENT '状态更新时间',
    status_updated_by   BIGINT        NULL     DEFAULT NULL   COMMENT '最近一次状态更新人',
    version             INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    created_at          DATETIME      NOT NULL                COMMENT '提交时间',
    updated_at          DATETIME      NOT NULL                COMMENT '更新时间',
    created_by          BIGINT        NULL     DEFAULT NULL   COMMENT '创建人 ID（匿名提交固定为系统占位值 0）',
    updated_by          BIGINT        NULL     DEFAULT NULL   COMMENT '更新人 ID',
    deleted_marker      BIGINT        NOT NULL DEFAULT 0      COMMENT '逻辑删除标记',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='线索记录';

-- 后台列表主路径：按提交时间倒序分页查询。
CREATE INDEX IF NOT EXISTS idx_cms_lead_record_del_submit_at
    ON cms_lead_record (deleted_marker, created_at, id);

-- 状态筛选是看板操作的高频条件。
CREATE INDEX IF NOT EXISTS idx_cms_lead_record_del_status_submit_at
    ON cms_lead_record (deleted_marker, status, created_at, id);

-- IP 索引用于排障和后续反刷分析。
CREATE INDEX IF NOT EXISTS idx_cms_lead_record_submit_ip_created_at
    ON cms_lead_record (submit_ip, created_at);
