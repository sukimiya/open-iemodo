-- =============================================================
-- V1: Create user tables in both tenant schemas
-- Flyway runs this for each schema configured in spring.flyway.schemas
-- =============================================================

-- ─── users ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                  BIGINT          PRIMARY KEY,    -- Snowflake ID
    tenant_id           VARCHAR(50)     NOT NULL,       -- Platform-level tenant identifier
    email               VARCHAR(255)    NOT NULL,
    password_hash       VARCHAR(255),                   -- NULL for OAuth2-only users
    
    -- Basic Profile
    display_name        VARCHAR(100),
    first_name          VARCHAR(100),
    last_name          VARCHAR(100),
    phone               VARCHAR(30),
    avatar_url          VARCHAR(500),
    
    -- OAuth2
    oauth_provider      VARCHAR(50)     NOT NULL DEFAULT 'LOCAL',  -- LOCAL | GOOGLE | FACEBOOK | APPLE
    oauth_subject       VARCHAR(255),                   -- Provider's unique user ID
    
    -- Verification
    email_verified      BOOLEAN         NOT NULL DEFAULT FALSE,
    phone_verified      BOOLEAN         NOT NULL DEFAULT FALSE,
    
    -- Preferences
    preferred_currency  VARCHAR(3)      NOT NULL DEFAULT 'USD',
    preferred_language  VARCHAR(10)     NOT NULL DEFAULT 'en',
    preferred_country   VARCHAR(2),
    
    -- Statistics
    total_orders        INTEGER         NOT NULL DEFAULT 0,
    total_spent         DECIMAL(15,2)   NOT NULL DEFAULT 0,
    
    -- Audit Fields (BaseEntity) - 所有子模块必须使用这些字段名
    status              INTEGER         NOT NULL DEFAULT 1,         -- 1=ACTIVE, 0=DISABLED
    create_by           BIGINT,                                     -- Creator user ID
    create_time         TIMESTAMPTZ,                                -- Creation time (UTC)
    update_by           BIGINT,                                     -- Updater user ID
    update_time         TIMESTAMPTZ,                                -- Update time (UTC)
    is_valid         BOOLEAN         NOT NULL DEFAULT true,         -- 1=Valid, 0=Soft deleted
    
    -- Legacy field for backward compatibility
    deleted_at          TIMESTAMPTZ,

    CONSTRAINT users_email_unique UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_users_tenant_id     ON users (tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_email         ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_oauth         ON users (oauth_provider, oauth_subject);
CREATE INDEX IF NOT EXISTS idx_users_status        ON users (status);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at    ON users (deleted_at) WHERE deleted_at IS NULL;

-- ─── refresh_tokens ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT          PRIMARY KEY,  -- Snowflake ID
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255)    NOT NULL,   -- SHA-256 hash of the raw token
    device_id   VARCHAR(100),               -- optional device fingerprint
    user_agent  VARCHAR(500),
    ip_address  VARCHAR(45),
    expires_at  TIMESTAMPTZ     NOT NULL,
    
    -- Audit Fields (BaseEntity)
    status      INTEGER         NOT NULL DEFAULT 1,
    create_by   BIGINT,
    create_time TIMESTAMPTZ,    -- UTC timestamp, set by BaseEntityBeforeSaveCallback
    update_by   BIGINT,
    update_time TIMESTAMPTZ,    -- UTC timestamp, set by BaseEntityBeforeSaveCallback
    is_valid         BOOLEAN         NOT NULL DEFAULT true,
    
    -- Legacy fields
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT refresh_tokens_hash_unique UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id    ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- ─── user_addresses ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_addresses (
    id                  BIGINT          PRIMARY KEY,  -- Snowflake ID
    customer_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Address Metadata
    address_name        VARCHAR(100),                   -- Alias: "Home", "Office"
    
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
    address_line1       VARCHAR(500)    NOT NULL,       -- Street address
    address_line2       VARCHAR(500),                   -- Apartment, suite, etc.
    postal_code         VARCHAR(20),
    
    -- Location & Verification
    geo_hash            VARCHAR(20),                    -- GeoHash encoding
    is_verified         BOOLEAN         NOT NULL DEFAULT FALSE,
    
    -- Default Flags
    is_default          BOOLEAN         NOT NULL DEFAULT FALSE,
    is_default_billing  BOOLEAN         NOT NULL DEFAULT FALSE,
    
    -- Audit Fields (BaseEntity)
    status              INTEGER         NOT NULL DEFAULT 1,
    create_by           BIGINT,
    create_time         TIMESTAMPTZ,    -- UTC timestamp
    update_by           BIGINT,
    update_time         TIMESTAMPTZ,    -- UTC timestamp
    is_valid         BOOLEAN         NOT NULL DEFAULT true
);

CREATE INDEX IF NOT EXISTS idx_user_addresses_customer_id ON user_addresses (customer_id);
CREATE INDEX IF NOT EXISTS idx_user_addresses_default     ON user_addresses (customer_id, is_default) WHERE is_default = TRUE;
CREATE INDEX IF NOT EXISTS idx_user_addresses_geo_hash    ON user_addresses (geo_hash) WHERE geo_hash IS NOT NULL;

-- ─── user_devices ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_devices (
    id              BIGINT          PRIMARY KEY,  -- Snowflake ID
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id       VARCHAR(100)    NOT NULL,           -- Device identifier
    device_type     VARCHAR(50),                        -- MOBILE | WEB | TABLET
    device_name     VARCHAR(200),                       -- Human-readable name
    user_agent      VARCHAR(500),
    ip_address      VARCHAR(45),
    last_seen_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_location   VARCHAR(200),                       -- Geographic location
    
    -- Audit Fields (BaseEntity)
    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_by       BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_valid         BOOLEAN         NOT NULL DEFAULT true,
    
    -- Legacy fields
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT user_devices_unique UNIQUE (user_id, device_id)
);

CREATE INDEX IF NOT EXISTS idx_user_devices_user_id    ON user_devices (user_id);
CREATE INDEX IF NOT EXISTS idx_user_devices_device_id  ON user_devices (device_id);

-- ─── oauth_connections ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS oauth_connections (
    id                  BIGINT          PRIMARY KEY,  -- Snowflake ID
    user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider            VARCHAR(50)     NOT NULL,       -- GOOGLE | FACEBOOK | APPLE
    provider_subject    VARCHAR(255)    NOT NULL,       -- Third-party user ID
    provider_email      VARCHAR(255),
    access_token        TEXT,
    refresh_token       TEXT,
    token_expires_at    TIMESTAMPTZ,
    
    -- Audit Fields (BaseEntity)
    status              INTEGER         NOT NULL DEFAULT 1,
    create_by           BIGINT,
    create_time         TIMESTAMPTZ,    -- UTC timestamp
    update_by           BIGINT,
    update_time         TIMESTAMPTZ,    -- UTC timestamp
    is_valid         BOOLEAN         NOT NULL DEFAULT true,

    CONSTRAINT oauth_connections_unique UNIQUE (provider, provider_subject)
);

CREATE INDEX IF NOT EXISTS idx_oauth_connections_user_id ON oauth_connections (user_id);
