CREATE TABLE IF NOT EXISTS cms_home_metric_card (
    id BIGINT NOT NULL AUTO_INCREMENT,
    metric_value VARCHAR(32) NOT NULL,
    metric_unit VARCHAR(32) NULL,
    description VARCHAR(120) NOT NULL,
    visible TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_cms_home_metric_card PRIMARY KEY (id),
    KEY idx_cms_home_metric_card_deleted_sort (deleted_marker, sort_order, id),
    KEY idx_cms_home_metric_card_visible_sort (deleted_marker, visible, sort_order, id)
);
