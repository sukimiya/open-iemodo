-- =============================================================
-- iemodo Database Initialization Script
-- Executed once on first PostgreSQL startup via docker-entrypoint
-- =============================================================

-- Create schemas for initial tenants (dev/test)
CREATE SCHEMA IF NOT EXISTS schema_tenant_001;
CREATE SCHEMA IF NOT EXISTS schema_tenant_002;

-- Grant privileges to iemodo user
GRANT ALL PRIVILEGES ON SCHEMA schema_tenant_001 TO iemodo;
GRANT ALL PRIVILEGES ON SCHEMA schema_tenant_002 TO iemodo;

-- Set default search path
ALTER DATABASE iemodo SET search_path TO public;
