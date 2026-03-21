-- =============================================================
-- V2: Add user_devices table for multi-device session management
-- =============================================================

CREATE TABLE IF NOT EXISTS user_devices (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id    VARCHAR(255) NOT NULL,
    device_name  VARCHAR(200),
    user_agent   VARCHAR(500),
    ip_address   VARCHAR(45),
    last_seen_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT user_devices_unique UNIQUE (user_id, device_id)
);

CREATE INDEX IF NOT EXISTS idx_user_devices_user_id ON user_devices (user_id);
CREATE INDEX IF NOT EXISTS idx_user_devices_device_id ON user_devices (device_id);
