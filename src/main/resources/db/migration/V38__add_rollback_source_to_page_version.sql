-- V38: 为页面版本历史表补充回滚来源版本ID字段，与产品/案例/行业方案版本表保持一致
ALTER TABLE cms_page_version
    ADD COLUMN rollback_source_version_id BIGINT NULL COMMENT '回滚来源版本ID，正常发布时为NULL，回滚发布时记录所基于的历史版本ID';
