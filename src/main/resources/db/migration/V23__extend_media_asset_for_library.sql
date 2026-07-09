ALTER TABLE media_asset
    ADD COLUMN usage_tag VARCHAR(32) NOT NULL DEFAULT 'OTHER';

ALTER TABLE media_asset
    ADD COLUMN alt_text VARCHAR(255) NULL;

ALTER TABLE media_asset
    ADD COLUMN remark VARCHAR(500) NULL;

CREATE INDEX idx_media_asset_usage_status_deleted_marker
    ON media_asset (usage_tag, status, deleted_marker);
