CREATE TABLE IF NOT EXISTS cms_navigation_menu (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_id BIGINT NOT NULL DEFAULT 0,
    menu_level TINYINT NOT NULL,
    menu_name VARCHAR(64) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    route_path VARCHAR(255) NULL,
    anchor_code VARCHAR(64) NULL,
    external_url VARCHAR(500) NULL,
    open_in_new_tab TINYINT NOT NULL DEFAULT 0,
    visible TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_cms_navigation_menu PRIMARY KEY (id),
    CONSTRAINT uk_cms_navigation_menu_parent_name_deleted_marker UNIQUE (parent_id, menu_name, deleted_marker)
);

CREATE INDEX idx_cms_navigation_menu_parent_deleted_sort
    ON cms_navigation_menu (parent_id, deleted_marker, sort_order, id);

CREATE INDEX idx_cms_navigation_menu_visible_deleted_sort
    ON cms_navigation_menu (visible, deleted_marker, sort_order, id);
