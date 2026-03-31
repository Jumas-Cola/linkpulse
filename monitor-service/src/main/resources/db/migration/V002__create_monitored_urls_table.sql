CREATE TABLE IF NOT EXISTS monitored_urls (
    id                   BIGSERIAL PRIMARY KEY,
    url                  VARCHAR(2048) NOT NULL,
    name                 VARCHAR(256)  NOT NULL,
    interval_seconds     INT           NOT NULL DEFAULT 60,
    owner_id             BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    current_status       VARCHAR(16)   NOT NULL DEFAULT 'UNKNOWN',
    consecutive_failures INT           NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_monitored_urls_owner ON monitored_urls(owner_id);
