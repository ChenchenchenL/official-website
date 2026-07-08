CREATE TABLE IF NOT EXISTS content_reference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    referrer_type VARCHAR(32) NOT NULL,
    referrer_key VARCHAR(128) NOT NULL,
    referenced_type VARCHAR(32) NOT NULL,
    referenced_id BIGINT NOT NULL,
    reference_type VARCHAR(64) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_content_reference PRIMARY KEY (id),
    CONSTRAINT uk_content_reference_unique_del UNIQUE (
        referrer_type,
        referrer_key,
        referenced_type,
        referenced_id,
        reference_type,
        deleted_marker
    )
);

CREATE INDEX idx_content_reference_referrer
    ON content_reference (deleted_marker, referrer_type, referrer_key, id);

CREATE INDEX idx_content_reference_referenced
    ON content_reference (deleted_marker, referenced_type, referenced_id, id);
