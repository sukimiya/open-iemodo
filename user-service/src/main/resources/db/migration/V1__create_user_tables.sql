-- =============================================================
-- V1: Create user tables in both tenant schemas
-- Flyway runs this for each schema configured in spring.flyway.schemas
-- =============================================================

-- ─── users ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL       PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255),                   -- NULL for OAuth2-only users
    display_name    VARCHAR(100),
    avatar_url      VARCHAR(500),
    oauth_provider  VARCHAR(50)     NOT NULL DEFAULT 'LOCAL',  -- LOCAL | GOOGLE | FACEBOOK
    oauth_subject   VARCHAR(255),                   -- Provider's unique user ID
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | DISABLED | DELETED
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT users_email_unique UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_users_email         ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_oauth         ON users (oauth_provider, oauth_subject);
CREATE INDEX IF NOT EXISTS idx_users_status        ON users (status);

-- ─── refresh_tokens ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255)    NOT NULL,   -- SHA-256 hash of the raw token
    device_id   VARCHAR(100),              -- optional device fingerprint
    user_agent  VARCHAR(500),
    ip_address  VARCHAR(45),
    expires_at  TIMESTAMPTZ     NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT refresh_tokens_hash_unique UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id    ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
