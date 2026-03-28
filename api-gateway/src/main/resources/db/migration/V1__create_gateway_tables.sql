-- =============================================================
-- V1: Create gateway configuration tables
-- Schema: gateway_config
-- =============================================================

-- ─── gateway_routes ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS gateway_routes (
    id              BIGSERIAL       PRIMARY KEY,
    route_id        VARCHAR(100)    NOT NULL UNIQUE,    -- Route identifier (e.g., "user-service-route")
    uri             VARCHAR(500)    NOT NULL,           -- Target URI (e.g., "lb://user-service")
    path            VARCHAR(500)    NOT NULL,           -- Path predicate (e.g., "/uc/**")
    method          VARCHAR(20),                        -- HTTP method filter (optional)
    priority        INTEGER         DEFAULT 0,          -- Route priority (lower = higher priority)
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    metadata        JSONB,                              -- Additional metadata
    description     VARCHAR(500),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gateway_routes_enabled ON gateway_routes (enabled);
CREATE INDEX IF NOT EXISTS idx_gateway_routes_priority ON gateway_routes (priority);

-- ─── rate_limit_rules ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS rate_limit_rules (
    id                  BIGSERIAL       PRIMARY KEY,
    rule_name           VARCHAR(100)    NOT NULL UNIQUE,
    route_id            VARCHAR(100),                       -- Associated route (optional)
    key_resolver        VARCHAR(50)     NOT NULL DEFAULT 'PRINCIPAL_NAME', -- PRINCIPAL_NAME | IP_ADDRESS | HEADER
    key_header          VARCHAR(100),                       -- Header name if key_resolver = HEADER
    replenish_rate      INTEGER         NOT NULL,           -- Tokens per second
    burst_capacity      INTEGER         NOT NULL,           -- Maximum bucket capacity
    requested_tokens    INTEGER         NOT NULL DEFAULT 1, -- Tokens consumed per request
    lua_script          TEXT,                               -- Custom Lua script (optional)
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    description         VARCHAR(500),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rate_limit_rules_route ON rate_limit_rules (route_id);
CREATE INDEX IF NOT EXISTS idx_rate_limit_rules_enabled ON rate_limit_rules (enabled);

-- ─── jwt_public_keys ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS jwt_public_keys (
    id              BIGSERIAL       PRIMARY KEY,
    key_id          VARCHAR(100)    NOT NULL UNIQUE,    -- Key identifier
    algorithm       VARCHAR(20)     NOT NULL DEFAULT 'RS256', -- RS256 | ES256
    public_key_pem  TEXT            NOT NULL,           -- Public key in PEM format
    issuer          VARCHAR(255),                       -- Expected JWT issuer
    audience        VARCHAR(255),                       -- Expected JWT audience
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    valid_from      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    valid_until     TIMESTAMPTZ,                        -- Key expiration
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_jwt_public_keys_active ON jwt_public_keys (active);

-- ─── gateway_access_logs ─────────────────────────────────────────────────────
-- Partitioned by range for efficient log rotation
CREATE TABLE IF NOT EXISTS gateway_access_logs (
    id              BIGSERIAL,
    request_id      VARCHAR(100),                       -- Unique request ID
    trace_id        VARCHAR(100),                       -- Distributed trace ID
    tenant_id       VARCHAR(50),                        -- Tenant identifier
    user_id         BIGINT,                             -- User ID if authenticated
    method          VARCHAR(10)     NOT NULL,
    path            VARCHAR(2000)   NOT NULL,
    query_params    VARCHAR(2000),
    client_ip       VARCHAR(45),
    user_agent      VARCHAR(500),
    status_code     INTEGER,
    response_time   INTEGER,                            -- Response time in milliseconds
    request_size    BIGINT,                             -- Request body size
    response_size   BIGINT,                             -- Response body size
    error_message   VARCHAR(1000),
    route_id        VARCHAR(100),
    target_uri      VARCHAR(500),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create initial partitions (monthly)
CREATE TABLE IF NOT EXISTS gateway_access_logs_y2024m03 PARTITION OF gateway_access_logs
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');
CREATE TABLE IF NOT EXISTS gateway_access_logs_y2024m04 PARTITION OF gateway_access_logs
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');
CREATE TABLE IF NOT EXISTS gateway_access_logs_y2024m05 PARTITION OF gateway_access_logs
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');
CREATE TABLE IF NOT EXISTS gateway_access_logs_y2024m06 PARTITION OF gateway_access_logs
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');

CREATE INDEX IF NOT EXISTS idx_access_logs_created_at ON gateway_access_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_access_logs_tenant ON gateway_access_logs (tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_access_logs_trace ON gateway_access_logs (trace_id);
CREATE INDEX IF NOT EXISTS idx_access_logs_status ON gateway_access_logs (status_code, created_at);

-- ─── Seed data: default routes ───────────────────────────────────────────────
INSERT INTO gateway_routes (route_id, uri, path, priority, description) VALUES
    ('user-service-route', 'lb://user-service', '/uc/**', 100, 'User Auth Service'),
    ('order-service-route', 'lb://order-service', '/oc/**', 100, 'Order Service'),
    ('tenant-management-route', 'lb://tenant-management-service', '/api/v1/tenants/**', 50, 'Tenant Management Service'),
    ('file-service-route', 'lb://file-service', '/api/v1/files/**', 100, 'File Storage Service')
ON CONFLICT (route_id) DO NOTHING;

-- ─── Seed data: default rate limit rules ─────────────────────────────────────
INSERT INTO rate_limit_rules (rule_name, key_resolver, replenish_rate, burst_capacity, description) VALUES
    ('default-public', 'IP_ADDRESS', 100, 200, 'Default public API rate limit'),
    ('default-authenticated', 'PRINCIPAL_NAME', 1000, 2000, 'Default authenticated API rate limit'),
    ('strict-auth', 'IP_ADDRESS', 10, 20, 'Strict limit for auth endpoints')
ON CONFLICT (rule_name) DO NOTHING;
