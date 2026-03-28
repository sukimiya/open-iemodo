-- Tax Service Schema
-- Schema: tax (shared schema for functional module)

-- Tax rates table
CREATE TABLE IF NOT EXISTS tax_rates (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    country_code VARCHAR(2) NOT NULL,
    region_code VARCHAR(10),
    county_code VARCHAR(20),
    city_code VARCHAR(20),
    postal_code_start VARCHAR(10),
    postal_code_end VARCHAR(10),
    postal_code_list TEXT[],
    tax_category VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    tax_type VARCHAR(20) NOT NULL,  -- VAT, GST, SALES_TAX, CONSUMPTION
    rate DECIMAL(5, 4) NOT NULL,    -- 0.2000 = 20%
    is_compound BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1,
    
    CONSTRAINT positive_rate CHECK (rate >= 0 AND rate <= 1)
);

-- Indexes for tax rates
CREATE INDEX idx_tax_rates_country ON tax_rates(country_code);
CREATE INDEX idx_tax_rates_country_region ON tax_rates(country_code, region_code);
CREATE INDEX idx_tax_rates_category ON tax_rates(tax_category);
CREATE INDEX idx_tax_rates_type ON tax_rates(tax_type);
CREATE INDEX idx_tax_rates_active ON tax_rates(is_active, effective_from, effective_to);

-- Insert common tax rates
INSERT INTO tax_rates (id, country_code, tax_type, tax_category, rate, effective_from) VALUES
    -- EU VAT rates
    (100001, 'DE', 'VAT', 'STANDARD', 0.1900, '2020-01-01'),
    (100002, 'DE', 'VAT', 'REDUCED', 0.0700, '2020-01-01'),
    (100003, 'FR', 'VAT', 'STANDARD', 0.2000, '2020-01-01'),
    (100004, 'FR', 'VAT', 'REDUCED', 0.0550, '2020-01-01'),
    (100005, 'GB', 'VAT', 'STANDARD', 0.2000, '2020-01-01'),
    (100006, 'GB', 'VAT', 'REDUCED', 0.0500, '2020-01-01'),
    (100007, 'IT', 'VAT', 'STANDARD', 0.2200, '2020-01-01'),
    (100008, 'ES', 'VAT', 'STANDARD', 0.2100, '2020-01-01'),
    (100009, 'NL', 'VAT', 'STANDARD', 0.2100, '2020-01-01'),
    
    -- GST rates
    (100010, 'AU', 'GST', 'STANDARD', 0.1000, '2020-01-01'),
    (100011, 'NZ', 'GST', 'STANDARD', 0.1500, '2020-01-01'),
    (100012, 'SG', 'GST', 'STANDARD', 0.0900, '2024-01-01'),
    (100013, 'CA', 'GST', 'STANDARD', 0.0500, '2020-01-01'),
    
    -- Consumption tax
    (100014, 'JP', 'CONSUMPTION', 'STANDARD', 0.1000, '2020-01-01'),
    
    -- US Sales Tax (示例 - 实际应按州/县/市)
    (100015, 'US', 'SALES_TAX', 'STANDARD', 0.0000, '2020-01-01')  -- 美国各州不同
ON CONFLICT DO NOTHING;

-- Product tax categories
CREATE TABLE IF NOT EXISTS product_tax_categories (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    category_code VARCHAR(50) NOT NULL UNIQUE,
    category_name VARCHAR(200) NOT NULL,
    description TEXT,
    default_rate DECIMAL(5,4),
    applicable_countries VARCHAR(2)[],
    hs_code_pattern VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1
);

INSERT INTO product_tax_categories (id, category_code, category_name, description, default_rate) VALUES
    (100001, 'STANDARD', 'Standard Rate', 'Standard taxable goods', 0.2000),
    (100002, 'REDUCED', 'Reduced Rate', 'Essential goods, food, books', 0.0500),
    (100003, 'ZERO', 'Zero Rate', 'Zero-rated goods', 0.0000),
    (100004, 'EXEMPT', 'Exempt', 'Tax exempt goods', NULL)
ON CONFLICT DO NOTHING;

-- Tax exemptions (B2B VAT exemption)
CREATE TABLE IF NOT EXISTS tax_exemptions (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    customer_id BIGINT NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    tax_id_number VARCHAR(100) NOT NULL,
    tax_id_type VARCHAR(20) NOT NULL,  -- VAT, EIN, ABN, etc.
    company_name VARCHAR(200),
    is_verified BOOLEAN DEFAULT FALSE,
    verification_source VARCHAR(50),     -- VIES, HMRC, MANUAL
    verification_response TEXT,
    valid_from DATE NOT NULL,
    valid_to DATE,
    exemption_status VARCHAR(20) DEFAULT 'PENDING_VERIFICATION', -- PENDING_VERIFICATION, ACTIVE, EXPIRED, REVOKED
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_tax_exemptions_customer ON tax_exemptions(customer_id);
CREATE INDEX idx_tax_exemptions_country ON tax_exemptions(country_code);
CREATE INDEX idx_tax_exemptions_status ON tax_exemptions(exemption_status);
CREATE INDEX idx_tax_exemptions_tenant ON tax_exemptions(tenant_id);

-- Tax transactions (for OSS/IOSS reporting)
CREATE TABLE IF NOT EXISTS tax_transactions (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    order_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    
    -- Destination
    country_code VARCHAR(2) NOT NULL,
    region_code VARCHAR(10),
    postal_code VARCHAR(20),
    
    -- Tax details
    tax_type VARCHAR(20) NOT NULL,
    tax_category VARCHAR(50),
    tax_rate DECIMAL(5, 4) NOT NULL,
    taxable_amount DECIMAL(19, 4) NOT NULL,
    tax_amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    
    -- For OSS/IOSS
    is_oss_applicable BOOLEAN DEFAULT FALSE,
    oss_country_code VARCHAR(2),
    reporting_period VARCHAR(7),  -- YYYY-MM format
    is_reported BOOLEAN DEFAULT FALSE,
    reported_at TIMESTAMP,
    
    -- Customer info
    customer_id BIGINT,
    customer_tax_id VARCHAR(100),
    is_b2b BOOLEAN DEFAULT FALSE,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_tax_transactions_order ON tax_transactions(order_id);
CREATE INDEX idx_tax_transactions_country ON tax_transactions(country_code);
CREATE INDEX idx_tax_transactions_period ON tax_transactions(reporting_period);
CREATE INDEX idx_tax_transactions_oss ON tax_transactions(is_oss_applicable, is_reported);
CREATE INDEX idx_tax_transactions_tenant ON tax_transactions(tenant_id);

-- Tax calculation audit log
CREATE TABLE IF NOT EXISTS tax_calculation_logs (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    calculation_request_id VARCHAR(64) NOT NULL,
    request_data JSONB,
    response_data JSONB,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_tax_logs_request ON tax_calculation_logs(calculation_request_id);
