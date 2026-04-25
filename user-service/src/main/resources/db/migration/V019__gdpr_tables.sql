-- =============================================================
-- V019: GDPR compliance tables (consent records + data retention)
-- =============================================================

-- ─── consent_records ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS consent_records (
    id              BIGINT          PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose         VARCHAR(50)     NOT NULL,        -- MARKETING | ANALYTICS | PERSONALIZATION | PROFILING | THIRD_PARTY
    consent_given   BOOLEAN         NOT NULL DEFAULT FALSE,
    consent_date    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    consent_version VARCHAR(20)     NOT NULL DEFAULT '1.0',
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),

    -- Audit Fields (BaseEntity)
    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ,
    update_by       BIGINT,
    update_time     TIMESTAMPTZ,
    is_valid        BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT consent_records_unique UNIQUE (user_id, purpose)
);

CREATE INDEX IF NOT EXISTS idx_consent_user       ON consent_records (user_id);
CREATE INDEX IF NOT EXISTS idx_consent_purpose     ON consent_records (purpose);

-- ─── data_retention_log ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS data_retention_log (
    id              BIGINT          PRIMARY KEY,
    batch_id        VARCHAR(50)     NOT NULL,
    data_type       VARCHAR(50)     NOT NULL,        -- EXPIRED_TOKENS | DELETED_USERS | OLD_LOGS | ANONYMIZED_USERS
    records_affected INTEGER        NOT NULL DEFAULT 0,
    started_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'   -- PENDING | RUNNING | COMPLETED | FAILED
);

CREATE INDEX IF NOT EXISTS idx_retention_batch     ON data_retention_log (batch_id);
CREATE INDEX IF NOT EXISTS idx_retention_type       ON data_retention_log (data_type);
