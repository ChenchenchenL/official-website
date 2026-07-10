ALTER TABLE cms_promise_tag
    ADD COLUMN description VARCHAR(255) NULL;

ALTER TABLE cms_promise_tag
    ADD COLUMN visible TINYINT(1) NOT NULL DEFAULT 1;

CREATE INDEX idx_cms_promise_tag_visible_del_sort
    ON cms_promise_tag (visible, deleted_marker, sort_order, id);
