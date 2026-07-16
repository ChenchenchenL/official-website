CREATE TABLE IF NOT EXISTS portal_cache_invalidation_retry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cache_keys TEXT NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME NOT NULL,
    last_error VARCHAR(1000) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_marker BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_portal_cache_invalidation_retry PRIMARY KEY (id)
);

CREATE INDEX idx_portal_cache_invalidation_retry_pending
    ON portal_cache_invalidation_retry (deleted_marker, status, next_retry_at);
