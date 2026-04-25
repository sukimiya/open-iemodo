-- Marketing Service Schema
-- Schema: marketing_{tenantId}

-- Coupons table
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    coupon_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    
    -- Coupon type
    coupon_type VARCHAR(20) NOT NULL, -- PERCENTAGE, FIXED_AMOUNT, FREE_SHIPPING
    discount_value DECIMAL(19, 4) NOT NULL, -- Percentage (0.20 = 20%) or fixed amount
    max_discount_amount DECIMAL(19, 4), -- Maximum discount cap for percentage coupons
    
    -- Usage limits
    min_order_amount DECIMAL(19, 4) DEFAULT 0,
    max_uses INT, -- NULL = unlimited
    max_uses_per_user INT DEFAULT 1,
    used_count INT DEFAULT 0,
    
    -- Applicable scope
    applicable_scope VARCHAR(20) DEFAULT 'ALL', -- ALL, CATEGORIES, PRODUCTS, BRANDS
    applicable_ids BIGINT[], -- IDs of applicable categories/products/brands
    excluded_ids BIGINT[], -- IDs to exclude
    
    -- Validity
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    
    -- Status
    coupon_active BOOLEAN DEFAULT TRUE,
    
    -- Tenant
    tenant_id VARCHAR(50) NOT NULL,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid         BOOLEAN         NOT NULL DEFAULT true
);

CREATE INDEX idx_coupons_code ON coupons(coupon_code);
CREATE INDEX idx_coupons_tenant ON coupons(tenant_id);
CREATE INDEX idx_coupons_active ON coupons(coupon_active, valid_from, valid_to);
CREATE INDEX idx_coupons_type ON coupons(coupon_type);

-- User coupons table
CREATE TABLE IF NOT EXISTS user_coupons (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    customer_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL REFERENCES coupons(id),
    coupon_code VARCHAR(50) NOT NULL,
    
    -- Status
    coupon_usage_status VARCHAR(20) DEFAULT 'UNUSED', -- UNUSED, USED, EXPIRED
    
    -- Usage info
    order_id BIGINT,
    order_no VARCHAR(32),
    used_at TIMESTAMP,
    discount_amount DECIMAL(19, 4),
    
    -- Validity (copied from coupon at claim time)
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    
    -- Tenant
    tenant_id VARCHAR(50) NOT NULL,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid         BOOLEAN         NOT NULL DEFAULT true,
    
    CONSTRAINT unique_user_coupon UNIQUE (customer_id, coupon_id)
);

CREATE INDEX idx_user_coupons_customer ON user_coupons(customer_id);
CREATE INDEX idx_user_coupons_status ON user_coupons(coupon_usage_status);
CREATE INDEX idx_user_coupons_tenant ON user_coupons(tenant_id);
CREATE INDEX idx_user_coupons_valid ON user_coupons(valid_to) WHERE coupon_usage_status = 'UNUSED';
