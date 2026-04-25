-- =============================================================
-- V1: Create tenant management tables in tenant_meta schema
-- =============================================================

-- ─── tenants ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenants (
    id              BIGINT          PRIMARY KEY,  -- Snowflake ID
    tenant_id       VARCHAR(50)     NOT NULL UNIQUE,
    tenant_name     VARCHAR(200)    NOT NULL,
    tenant_code     VARCHAR(100)    NOT NULL UNIQUE,
    tenant_status   VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    plan_type       VARCHAR(50)     NOT NULL DEFAULT 'STANDARD',
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(30),
    db_host         VARCHAR(200)    DEFAULT 'localhost',
    db_name         VARCHAR(100)    DEFAULT 'iemodo',
    
    -- BaseEntity audit fields
    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ,
    update_by       BIGINT,
    update_time     TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true,

    CONSTRAINT tenants_status_check CHECK (tenant_status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX IF NOT EXISTS idx_tenants_tenant_status ON tenants (tenant_status);
CREATE INDEX IF NOT EXISTS idx_tenants_code ON tenants (tenant_code);

-- ─── tenant_schemas ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenant_schemas (
    id              BIGINT          PRIMARY KEY,  -- Snowflake ID
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    service_name    VARCHAR(50)     NOT NULL,
    schema_name     VARCHAR(100)    NOT NULL,
    connection_pool VARCHAR(50)     DEFAULT 'default',
    
    -- BaseEntity audit fields
    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ,
    update_by       BIGINT,
    update_time     TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true,

    CONSTRAINT tenant_schemas_unique UNIQUE (tenant_id, service_name)
);

CREATE INDEX IF NOT EXISTS idx_tenant_schemas_tenant ON tenant_schemas (tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_schemas_service ON tenant_schemas (service_name);

-- ─── tenant_configs ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenant_configs (
    id              BIGINT          PRIMARY KEY,  -- Snowflake ID
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    config_key      VARCHAR(200)    NOT NULL,
    config_value    TEXT,
    config_type     VARCHAR(20)     NOT NULL DEFAULT 'STRING',
    description     VARCHAR(500),
    editable        BOOLEAN         NOT NULL DEFAULT TRUE,
    
    -- BaseEntity audit fields
    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ,
    update_by       BIGINT,
    update_time     TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true,

    CONSTRAINT tenant_configs_unique UNIQUE (tenant_id, config_key),
    CONSTRAINT tenant_configs_type_check CHECK (config_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'JSON'))
);

CREATE INDEX IF NOT EXISTS idx_tenant_configs_tenant ON tenant_configs (tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_configs_key ON tenant_configs (config_key);

-- ─── Seed data: system tenant ────────────────────────────────────────────────
INSERT INTO tenants (id, tenant_id, tenant_name, tenant_code, tenant_status, plan_type)
VALUES (100001, 'system', 'System Tenant', 'system', 'ACTIVE', 'ENTERPRISE')
ON CONFLICT (tenant_id) DO NOTHING;

-- ─── Seed data: demo tenant ──────────────────────────────────────────────────
INSERT INTO tenants (id, tenant_id, tenant_name, tenant_code, tenant_status, plan_type, contact_email)
VALUES (100002, 'demo', 'Demo Tenant', 'demo', 'ACTIVE', 'STANDARD', 'demo@example.com')
ON CONFLICT (tenant_id) DO NOTHING;

-- Create schema mappings for demo tenant
INSERT INTO tenant_schemas (id, tenant_id, service_name, schema_name)
SELECT 
    100000 + row_number() OVER () as id,
    'demo' as tenant_id, 
    service as service_name, 
    'demo_' || service as schema_name
FROM (VALUES 
    ('user_auth'), ('product'), ('order'), ('inventory'), 
    ('payment'), ('pricing'), ('tax'), ('marketing'), ('fulfillment')
) AS services(service)
ON CONFLICT (tenant_id, service_name) DO NOTHING;

-- Create default configs for demo tenant
INSERT INTO tenant_configs (id, tenant_id, config_key, config_value, config_type, description)
VALUES 
    (100001, 'demo', 'default.currency', 'USD', 'STRING', 'Default currency for transactions'),
    (100002, 'demo', 'default.language', 'en', 'STRING', 'Default language for UI'),
    (100003, 'demo', 'default.country', 'US', 'STRING', 'Default country for shipping'),
    (100004, 'demo', 'tax.rate', '0.08', 'DECIMAL', 'Default tax rate'),
    (100005, 'demo', 'timezone', 'America/New_York', 'STRING', 'Default timezone'),
    (100006, 'demo', 'date.format', 'MM/dd/yyyy', 'STRING', 'Date format for display')
ON CONFLICT (tenant_id, config_key) DO NOTHING;
