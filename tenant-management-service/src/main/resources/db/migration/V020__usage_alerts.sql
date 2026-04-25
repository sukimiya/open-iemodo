-- =============================================================
-- V020: Usage alert tracking tables
-- =============================================================

-- ─── usage_alert_log ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS usage_alert_log (
    id              BIGINT          PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL,
    metric          VARCHAR(50)     NOT NULL,
    threshold_pct   INTEGER         NOT NULL,         -- 80, 90, 100
    current_usage   BIGINT          NOT NULL,
    limit_value     BIGINT          NOT NULL,
    alert_sent_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    notified_via    VARCHAR(50),                      -- EMAIL | SMS | PUSH

    -- BaseEntity audit fields
    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ,
    update_by       BIGINT,
    update_time     TIMESTAMPTZ,
    is_valid        BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_alert_log_tenant   ON usage_alert_log (tenant_id);
CREATE INDEX IF NOT EXISTS idx_alert_log_sent_at  ON usage_alert_log (alert_sent_at);
CREATE INDEX IF NOT EXISTS idx_alert_log_metric   ON usage_alert_log (tenant_id, metric, alert_sent_at);
