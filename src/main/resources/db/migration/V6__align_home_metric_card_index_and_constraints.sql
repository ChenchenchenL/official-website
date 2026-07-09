DROP INDEX idx_cms_home_metric_card_visible_sort
ON cms_home_metric_card;

CREATE INDEX idx_cms_home_metric_card_visible_sort
    ON cms_home_metric_card (deleted_marker, visible, sort_order, id);

ALTER TABLE cms_home_metric_card
    ADD CONSTRAINT chk_cms_home_metric_card_visible CHECK (visible IN (0, 1));