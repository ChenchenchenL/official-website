DROP INDEX IF EXISTS idx_cms_case_visible_del ON cms_case;

CREATE INDEX IF NOT EXISTS idx_cms_case_del_visible_sort_id
    ON cms_case (deleted_marker, visible, sort_order, id);
