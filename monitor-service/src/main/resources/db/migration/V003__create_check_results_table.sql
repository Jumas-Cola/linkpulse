CREATE TABLE IF NOT EXISTS check_results (
    id          BIGSERIAL PRIMARY KEY,
    url_id      BIGINT      NOT NULL REFERENCES monitored_urls(id) ON DELETE CASCADE,
    http_status INT,
    latency_ms  BIGINT      NOT NULL,
    error       TEXT,
    checked_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_check_results_url_id ON check_results(url_id);
CREATE INDEX idx_check_results_checked_at ON check_results(checked_at DESC);
