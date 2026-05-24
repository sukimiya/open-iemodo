-- ─────────────────────────────────────────────────────────────────────────────
-- customers
--   Holds end-customer identities independent from admin users.
--   Multiple login methods: phone (SMS OTP), email (password), OAuth2.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customers (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,

    -- Login identifiers (at least one must be non-null per CHECK constraint)
    phone               VARCHAR(30),
    email               VARCHAR(255),
    oauth_provider      VARCHAR(50),      -- GOOGLE | FACEBOOK | APPLE | WECHAT
    oauth_subject       VARCHAR(255),     -- Provider's unique user ID

    -- Password (only for email+password login)
    password_hash       VARCHAR(255),

    -- Profile
    display_name        VARCHAR(100),
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    avatar_url          VARCHAR(500),

    -- Verification
    phone_verified      BOOLEAN         NOT NULL DEFAULT FALSE,
    email_verified      BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Preferences
    preferred_currency  VARCHAR(3)      NOT NULL DEFAULT 'USD',
    preferred_language  VARCHAR(10)     NOT NULL DEFAULT 'en',
    preferred_country   VARCHAR(2),

    -- Last login info
    last_login_at       TIMESTAMPTZ,
    last_login_ip       VARCHAR(45),

    -- BaseEntity audit columns
    status              INTEGER         NOT NULL DEFAULT 1,
    create_by           BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_by           BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_valid            BOOLEAN         NOT NULL DEFAULT TRUE
);

-- At least one login method must be present
ALTER TABLE customers ADD CONSTRAINT chk_customer_login_method
    CHECK (phone IS NOT NULL OR email IS NOT NULL OR (oauth_provider IS NOT NULL AND oauth_subject IS NOT NULL));

-- Unique indexes (partial, to avoid conflicts on NULLs)
CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_phone
    ON customers (tenant_id, phone) WHERE phone IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_email
    ON customers (tenant_id, email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_oauth
    ON customers (oauth_provider, oauth_subject) WHERE oauth_subject IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_customers_tenant
    ON customers (tenant_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- customer_refresh_tokens
--   Opaque refresh tokens for customer JWT rotation.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customer_refresh_tokens (
    id              BIGINT          PRIMARY KEY,
    customer_id     BIGINT          NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255)    NOT NULL,
    device_id       VARCHAR(100),
    user_agent      VARCHAR(500),
    ip_address      VARCHAR(45),
    expires_at      TIMESTAMPTZ     NOT NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,

    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_by       BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_valid        BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_customer_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_cust_rf_customer
    ON customer_refresh_tokens (customer_id);
CREATE INDEX IF NOT EXISTS idx_cust_rf_expires
    ON customer_refresh_tokens (expires_at);

-- ─────────────────────────────────────────────────────────────────────────────
-- customer_otp_records
--   Audit log for SMS OTP sends and verifications.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customer_otp_records (
    id              BIGINT          PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL,
    phone           VARCHAR(30)     NOT NULL,
    otp_hash        VARCHAR(255)    NOT NULL,
    purpose         VARCHAR(20)     NOT NULL DEFAULT 'LOGIN',  -- LOGIN | VERIFY_PHONE
    verified        BOOLEAN         NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMPTZ     NOT NULL,
    ip_address      VARCHAR(45),

    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_by       BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_valid        BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_otp_phone
    ON customer_otp_records (tenant_id, phone, create_time DESC);

-- ─────────────────────────────────────────────────────────────────────────────
-- customer_oauth_connections
--   Links a customer to multiple OAuth2 provider identities.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customer_oauth_connections (
    id                  BIGINT          PRIMARY KEY,
    customer_id         BIGINT          NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    provider            VARCHAR(50)     NOT NULL,     -- GOOGLE | FACEBOOK | APPLE | WECHAT
    provider_subject    VARCHAR(255)    NOT NULL,
    provider_email      VARCHAR(255),
    access_token        TEXT,
    refresh_token       TEXT,
    id_token            TEXT,
    token_expires_at    TIMESTAMPTZ,

    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_by       BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_valid        BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_customer_oauth UNIQUE (provider, provider_subject)
);

CREATE INDEX IF NOT EXISTS idx_cust_oauth_customer
    ON customer_oauth_connections (customer_id);
