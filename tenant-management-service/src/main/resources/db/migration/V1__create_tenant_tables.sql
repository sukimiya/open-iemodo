-- =============================================================
-- V1: Create tenant management tables in tenant_meta schema
-- =============================================================

-- ─── tenants ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenants (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL UNIQUE,
    tenant_name     VARCHAR(200)    NOT NULL,
    tenant_code     VARCHAR(100)    NOT NULL UNIQUE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    plan_type       VARCHAR(50)     NOT NULL DEFAULT 'STANDARD',
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(30),
    db_host         VARCHAR(200)    DEFAULT 'localhost',
    db_name         VARCHAR(100)    DEFAULT 'iemodo',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT tenants_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants (status);
CREATE INDEX IF NOT EXISTS idx_tenants_code ON tenants (tenant_code);

-- ─── tenant_schemas ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenant_schemas (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    service_name    VARCHAR(50)     NOT NULL,
    schema_name     VARCHAR(100)    NOT NULL,
    connection_pool VARCHAR(50)     DEFAULT 'default',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT tenant_schemas_unique UNIQUE (tenant_id, service_name)
);

CREATE INDEX IF NOT EXISTS idx_tenant_schemas_tenant ON tenant_schemas (tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_schemas_service ON tenant_schemas (service_name);

-- ─── tenant_configs ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenant_configs (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    config_key      VARCHAR(200)    NOT NULL,
    config_value    TEXT,
    config_type     VARCHAR(20)     NOT NULL DEFAULT 'STRING',
    description     VARCHAR(500),
    editable        BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT tenant_configs_unique UNIQUE (tenant_id, config_key),
    CONSTRAINT tenant_configs_type_check CHECK (config_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'JSON'))
);

CREATE INDEX IF NOT EXISTS idx_tenant_configs_tenant ON tenant_configs (tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_configs_key ON tenant_configs (config_key);

-- ─── Seed data: system tenant ────────────────────────────────────────────────
INSERT INTO tenants (tenant_id, tenant_name, tenant_code, status, plan_type)
VALUES ('system', 'System Tenant', 'system', 'ACTIVE', 'ENTERPRISE')
ON CONFLICT (tenant_id) DO NOTHING;

-- ─── Seed data: demo tenant ──────────────────────────────────────────────────
INSERT INTO tenants (tenant_id, tenant_name, tenant_code, status, plan_type, contact_email)
VALUES ('demo', 'Demo Tenant', 'demo', 'ACTIVE', 'STANDARD', 'demo@example.com')
ON CONFLICT (tenant_id) DO NOTHING;

-- Create schema mappings for demo tenant
INSERT INTO tenant_schemas (tenant_id, service_name, schema_name)
SELECT 'demo', service, 'demo_' || service
FROM (VALUES 
    ('user_auth'), ('product'), ('order'), ('inventory'), 
    ('payment'), ('pricing'), ('tax'), ('marketing'), ('fulfillment')
) AS services(service)
ON CONFLICT (tenant_id, service_name) DO NOTHING;

-- Create default configs for demo tenant
INSERT INTO tenant_configs (tenant_id, config_key, config_value, config_type, description)
VALUES 
    ('demo', 'default.currency', 'USD', 'STRING', 'Default currency for transactions'),
    ('demo', 'default.language', 'en', 'STRING', 'Default language for UI'),
    ('demo', 'default.country', 'US', 'STRING', 'Default country for shipping'),
    ('demo', 'tax.rate', '0.08', 'DECIMAL', 'Default tax rate'),
    ('demo', 'timezone', 'America/New_York', 'STRING', 'Default timezone'),
    ('demo', 'date.format', 'MM/dd/yyyy', 'STRING', 'Date format for display')
ON CONFLICT (tenant_id, config_key) DO NOTHING;
