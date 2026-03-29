-- =============================================================
-- V1: RMA (Return Merchandise Authorization) tables
-- =============================================================

-- ─── rma_region_configs ──────────────────────────────────────────────────────
-- Stores per-region RMA policy rules.
-- null tenant_id = platform default; non-null = tenant override.
CREATE TABLE IF NOT EXISTS rma_region_configs (
    id                      BIGINT          PRIMARY KEY,
    region_code             VARCHAR(20)     NOT NULL,       -- EU, US, JP, AU ...
    tenant_id               VARCHAR(50),                    -- NULL = platform default

    return_window_days      INTEGER         NOT NULL DEFAULT 30,
    exchange_window_days    INTEGER         NOT NULL DEFAULT 30,
    shipping_responsibility VARCHAR(20)     NOT NULL DEFAULT 'NEGOTIABLE',  -- BUYER|SELLER|NEGOTIABLE
    tax_refund_policy       VARCHAR(20)     NOT NULL DEFAULT 'IF_NOT_SHIPPED', -- NEVER|IF_NOT_SHIPPED|ALWAYS
    require_reason          BOOLEAN         NOT NULL DEFAULT TRUE,
    auto_approve_threshold  DECIMAL(12,2),                  -- NULL = no auto-approve
    auto_approve_currency   VARCHAR(3),

    status      INTEGER     NOT NULL DEFAULT 1,
    create_by   BIGINT,
    create_time TIMESTAMPTZ,
    update_by   BIGINT,
    update_time TIMESTAMPTZ,
    is_valid    INTEGER     NOT NULL DEFAULT 1,

    CONSTRAINT rma_region_configs_unique UNIQUE (region_code, tenant_id),
    CONSTRAINT rma_shipping_responsibility_check
        CHECK (shipping_responsibility IN ('BUYER', 'SELLER', 'NEGOTIABLE')),
    CONSTRAINT rma_tax_refund_policy_check
        CHECK (tax_refund_policy IN ('NEVER', 'IF_NOT_SHIPPED', 'ALWAYS'))
);

CREATE INDEX IF NOT EXISTS idx_rma_region_configs_region ON rma_region_configs (region_code);
CREATE INDEX IF NOT EXISTS idx_rma_region_configs_tenant ON rma_region_configs (tenant_id);

-- ─── rma_requests ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS rma_requests (
    id                  BIGINT          PRIMARY KEY,
    rma_no              VARCHAR(30)     NOT NULL UNIQUE,
    tenant_id           VARCHAR(50)     NOT NULL,
    order_id            BIGINT          NOT NULL,
    customer_id         BIGINT          NOT NULL,

    type                VARCHAR(20)     NOT NULL,   -- RETURN | EXCHANGE | REFUND_ONLY
    rma_status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING_REVIEW',

    -- Region policy (snapshot stored at creation time)
    region_code         VARCHAR(20)     NOT NULL,
    region_snapshot     TEXT            NOT NULL,   -- JSON snapshot of rma_region_configs row

    -- Customer input
    reason              VARCHAR(200)    NOT NULL,
    description         TEXT,

    -- Refund details (populated at APPROVED time)
    refund_amount       DECIMAL(12,2),
    refund_currency     VARCHAR(3),
    tax_refund_included BOOLEAN,

    -- Return logistics (buyer fills in after approval)
    tracking_no         VARCHAR(100),
    carrier             VARCHAR(50),

    -- Operator
    merchant_notes      TEXT,
    last_operator_id    BIGINT,

    -- Convenience timestamps
    approved_at         TIMESTAMPTZ,
    received_at         TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,

    -- BaseEntity
    status      INTEGER     NOT NULL DEFAULT 1,
    create_by   BIGINT,
    create_time TIMESTAMPTZ,
    update_by   BIGINT,
    update_time TIMESTAMPTZ,
    is_valid    INTEGER     NOT NULL DEFAULT 1,

    CONSTRAINT rma_type_check
        CHECK (type IN ('RETURN', 'EXCHANGE', 'REFUND_ONLY')),
    CONSTRAINT rma_status_check
        CHECK (rma_status IN ('PENDING_REVIEW','APPROVED','REJECTED','WAITING_SHIPMENT',
                              'IN_TRANSIT','RECEIVED','INSPECTING','REFUNDING',
                              'RESHIPPING','COMPLETED','CANCELLED','FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_rma_requests_tenant      ON rma_requests (tenant_id);
CREATE INDEX IF NOT EXISTS idx_rma_requests_order       ON rma_requests (order_id);
CREATE INDEX IF NOT EXISTS idx_rma_requests_customer    ON rma_requests (customer_id);
CREATE INDEX IF NOT EXISTS idx_rma_requests_status      ON rma_requests (rma_status);
CREATE INDEX IF NOT EXISTS idx_rma_requests_create_time ON rma_requests (create_time DESC);

-- ─── rma_items ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS rma_items (
    id              BIGINT          PRIMARY KEY,
    rma_id          BIGINT          NOT NULL REFERENCES rma_requests(id) ON DELETE CASCADE,
    order_item_id   BIGINT          NOT NULL,
    product_id      BIGINT          NOT NULL,
    sku             VARCHAR(100)    NOT NULL,
    quantity        INTEGER         NOT NULL CHECK (quantity > 0),
    unit_price      DECIMAL(12,2)   NOT NULL,
    reason          VARCHAR(200),
    condition       VARCHAR(20),    -- UNOPENED | USED | DAMAGED (set by warehouse on receipt)

    status      INTEGER     NOT NULL DEFAULT 1,
    create_by   BIGINT,
    create_time TIMESTAMPTZ,
    update_by   BIGINT,
    update_time TIMESTAMPTZ,
    is_valid    INTEGER     NOT NULL DEFAULT 1,

    CONSTRAINT rma_items_condition_check
        CHECK (condition IS NULL OR condition IN ('UNOPENED', 'USED', 'DAMAGED'))
);

CREATE INDEX IF NOT EXISTS idx_rma_items_rma_id ON rma_items (rma_id);

-- ─── rma_status_history ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS rma_status_history (
    id              BIGINT          PRIMARY KEY,
    rma_id          BIGINT          NOT NULL REFERENCES rma_requests(id) ON DELETE CASCADE,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30)     NOT NULL,
    operator_id     BIGINT,
    operator_type   VARCHAR(20),    -- CUSTOMER | MERCHANT | SYSTEM
    remark          TEXT,

    status      INTEGER     NOT NULL DEFAULT 1,
    create_by   BIGINT,
    create_time TIMESTAMPTZ DEFAULT NOW(),
    update_by   BIGINT,
    update_time TIMESTAMPTZ,
    is_valid    INTEGER     NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_rma_status_history_rma_id ON rma_status_history (rma_id);

-- ─── Seed: platform-default region configs ───────────────────────────────────
INSERT INTO rma_region_configs (
    id, region_code, tenant_id,
    return_window_days, exchange_window_days,
    shipping_responsibility, tax_refund_policy,
    require_reason, auto_approve_threshold, auto_approve_currency
) VALUES
    -- EU: 14-day statutory right, seller bears return shipping for defects
    (1, 'EU', NULL, 14, 14, 'SELLER', 'IF_NOT_SHIPPED', TRUE, 20.00, 'EUR'),
    -- US: 30-day standard, negotiable shipping
    (2, 'US', NULL, 30, 30, 'NEGOTIABLE', 'IF_NOT_SHIPPED', TRUE, 30.00, 'USD'),
    -- JP: 7-day, buyer bears cost
    (3, 'JP', NULL, 7,  7,  'BUYER',      'IF_NOT_SHIPPED', TRUE, NULL, NULL),
    -- AU: 30-day
    (4, 'AU', NULL, 30, 30, 'NEGOTIABLE', 'IF_NOT_SHIPPED', TRUE, 25.00, 'AUD'),
    -- Global fallback
    (5, 'GLOBAL', NULL, 30, 30, 'NEGOTIABLE', 'IF_NOT_SHIPPED', TRUE, NULL, NULL)
ON CONFLICT (region_code, tenant_id) DO NOTHING;
