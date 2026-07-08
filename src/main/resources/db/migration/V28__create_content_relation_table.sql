CREATE TABLE IF NOT EXISTS content_relation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    relation_type VARCHAR(64) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_content_relation PRIMARY KEY (id),
    CONSTRAINT uk_content_relation_unique_del UNIQUE (
        source_type,
        source_id,
        target_type,
        target_id,
        relation_type,
        deleted_marker
    )
);

CREATE INDEX idx_content_relation_source
    ON content_relation (deleted_marker, source_type, source_id, relation_type, id);

CREATE INDEX idx_content_relation_target
    ON content_relation (deleted_marker, target_type, target_id, relation_type, id);
