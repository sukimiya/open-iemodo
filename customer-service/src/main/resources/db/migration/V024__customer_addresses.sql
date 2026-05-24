-- ─────────────────────────────────────────────────────────────────────────────
-- customer_addresses
--   Customer address book for shipping and billing.
--   Each customer can have multiple addresses with a single default.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customer_addresses (
    id                  BIGINT          PRIMARY KEY,
    customer_id         BIGINT          NOT NULL REFERENCES customers(id) ON DELETE CASCADE,

    -- Address Metadata
    address_name        VARCHAR(100),                   -- "Home", "Office", etc.

    -- Recipient Information
    recipient_name      VARCHAR(100)    NOT NULL,
    recipient_phone     VARCHAR(30)     NOT NULL,
    recipient_email     VARCHAR(200),

    -- Address Components
    country_code        VARCHAR(2)      NOT NULL,
    region_code         VARCHAR(10),                    -- State/Province code
    region_name         VARCHAR(100),                   -- State/Province name
    city                VARCHAR(100)    NOT NULL,
    district            VARCHAR(100),                   -- District/County
    address_line1       VARCHAR(500)    NOT NULL,
    address_line2       VARCHAR(500),                   -- Apartment, suite, etc.
    postal_code         VARCHAR(20),

    -- Location & Verification
    geo_hash            VARCHAR(20),
    is_verified         BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Default Flags
    is_default          BOOLEAN         NOT NULL DEFAULT FALSE,
    is_default_billing  BOOLEAN         NOT NULL DEFAULT FALSE,

    -- BaseEntity audit columns
    status              INTEGER         NOT NULL DEFAULT 1,
    create_by           BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_by           BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_valid            BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_cust_addr_customer
    ON customer_addresses (customer_id);

CREATE INDEX IF NOT EXISTS idx_cust_addr_default
    ON customer_addresses (customer_id, is_default);
