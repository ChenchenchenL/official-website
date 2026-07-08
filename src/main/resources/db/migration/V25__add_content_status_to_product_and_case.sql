ALTER TABLE cms_product
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '内容状态：DRAFT/PUBLISHED/OFFLINE' AFTER status_tag;

CREATE INDEX idx_cms_product_del_visible_status_sort
    ON cms_product (deleted_marker, visible, status, sort_order, id);

ALTER TABLE cms_case
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '内容状态：DRAFT/PUBLISHED/OFFLINE' AFTER visible;

CREATE INDEX idx_cms_case_del_visible_status_sort
    ON cms_case (deleted_marker, visible, status, sort_order, id);
