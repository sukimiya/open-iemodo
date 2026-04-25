-- =============================================================
-- V018: Create billing and usage metering tables in tenant_meta
-- =============================================================

-- ─── tenant_subscriptions ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenant_subscriptions (
    id                      BIGINT          PRIMARY KEY,
    tenant_id               VARCHAR(50)     NOT NULL UNIQUE,
    stripe_subscription_id  VARCHAR(100),
    stripe_customer_id      VARCHAR(100),
    plan_id                 VARCHAR(50)     NOT NULL DEFAULT 'STANDARD',
    subscription_status     VARCHAR(20)     NOT NULL DEFAULT 'INCOMPLETE',
    current_period_start    TIMESTAMPTZ,
    current_period_end      TIMESTAMPTZ,
    cancel_at_period_end    BOOLEAN         NOT NULL DEFAULT FALSE,
    last_invoice_id         VARCHAR(100),
    billing_cycle_count     INTEGER         NOT NULL DEFAULT 0,

    -- BaseEntity audit fields
    status                  INTEGER         NOT NULL DEFAULT 1,
    create_by               BIGINT,
    create_time             TIMESTAMPTZ,
    update_by               BIGINT,
    update_time             TIMESTAMPTZ,
    is_valid                BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_tenant ON tenant_subscriptions (tenant_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_stripe_sub ON tenant_subscriptions (stripe_subscription_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_stripe_cust ON tenant_subscriptions (stripe_customer_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON tenant_subscriptions (subscription_status);

-- ─── usage_records ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS usage_records (
    id              BIGINT          PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL,
    usage_date      DATE            NOT NULL,
    metric          VARCHAR(50)     NOT NULL,
    count_value     BIGINT          NOT NULL DEFAULT 0,

    -- BaseEntity audit fields
    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ,
    update_by       BIGINT,
    update_time     TIMESTAMPTZ,
    is_valid        BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT usage_records_unique UNIQUE (tenant_id, usage_date, metric)
);

CREATE INDEX IF NOT EXISTS idx_usage_tenant_date ON usage_records (tenant_id, usage_date);
CREATE INDEX IF NOT EXISTS idx_usage_metric ON usage_records (metric);

-- ─── Seed subscription for demo tenant ─────────────────────────────
INSERT INTO tenant_subscriptions (id, tenant_id, stripe_subscription_id, stripe_customer_id,
                                  plan_id, subscription_status, current_period_start, current_period_end)
VALUES (200001, 'demo', NULL, NULL, 'STANDARD', 'ACTIVE',
        NOW(), NOW() + INTERVAL '1 year')
ON CONFLICT (tenant_id) DO NOTHING;

-- ─── Seed subscription for system tenant ────────────────────────────
INSERT INTO tenant_subscriptions (id, tenant_id, stripe_subscription_id, stripe_customer_id,
                                  plan_id, subscription_status, current_period_start, current_period_end)
VALUES (200002, 'system', NULL, NULL, 'ENTERPRISE', 'ACTIVE',
        NOW(), NOW() + INTERVAL '100 years')
ON CONFLICT (tenant_id) DO NOTHING;
