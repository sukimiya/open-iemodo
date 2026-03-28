-- Fulfillment Service Schema
-- Schema: fulfillment_{tenantId}

-- Warehouse evaluations cache
CREATE TABLE IF NOT EXISTS warehouse_evaluations (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    warehouse_id BIGINT NOT NULL,
    destination_country VARCHAR(2) NOT NULL,
    destination_region VARCHAR(50),
    destination_postal_code VARCHAR(20),
    
    -- Evaluation scores
    cost_score DECIMAL(3, 2),
    speed_score DECIMAL(3, 2),
    availability_score DECIMAL(3, 2),
    service_level_score DECIMAL(3, 2),
    composite_score DECIMAL(5, 4),
    
    -- Calculated values
    estimated_shipping_cost DECIMAL(19, 4),
    estimated_delivery_days INT,
    stock_availability DECIMAL(5, 2), -- percentage
    
    -- Metadata
    evaluation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evaluation_active BOOLEAN DEFAULT TRUE,
    
    -- Tenant
    tenant_id VARCHAR(50) NOT NULL,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1,
    
    CONSTRAINT unique_warehouse_evaluation UNIQUE (warehouse_id, destination_country, destination_postal_code)
);

CREATE INDEX idx_warehouse_evaluations_warehouse ON warehouse_evaluations(warehouse_id);
CREATE INDEX idx_warehouse_evaluations_destination ON warehouse_evaluations(destination_country, destination_region);
CREATE INDEX idx_warehouse_evaluations_score ON warehouse_evaluations(composite_score DESC);

-- Stock transfer recommendations
CREATE TABLE IF NOT EXISTS stock_transfer_recommendations (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    recommendation_no VARCHAR(32) UNIQUE NOT NULL,
    
    -- Source and destination
    from_warehouse_id BIGINT NOT NULL,
    to_warehouse_id BIGINT NOT NULL,
    
    -- Analysis parameters
    analysis_type VARCHAR(20) NOT NULL, -- COUNTRY, REGION, SKU
    lookback_days INT NOT NULL,
    
    -- Recommendation details
    sku_id BIGINT,
    sku VARCHAR(100),
    recommended_quantity INT NOT NULL,
    reason TEXT,
    recommendation_priority VARCHAR(20) DEFAULT 'MEDIUM', -- HIGH, MEDIUM, LOW
    
    -- Expected impact
    projected_cost_savings DECIMAL(19, 4),
    projected_delivery_improvement DECIMAL(5, 2), -- percentage
    
    -- Status
    recommendation_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, EXECUTED
    approved_by BIGINT,
    approved_at TIMESTAMP,
    executed_at TIMESTAMP,
    
    -- Tenant
    tenant_id VARCHAR(50) NOT NULL,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_transfer_recommendations_status ON stock_transfer_recommendations(recommendation_status);
CREATE INDEX idx_transfer_recommendations_tenant ON stock_transfer_recommendations(tenant_id);
CREATE INDEX idx_transfer_recommendations_priority ON stock_transfer_recommendations(recommendation_priority);

-- Delivery time estimates cache
CREATE TABLE IF NOT EXISTS delivery_time_estimates (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    warehouse_id BIGINT NOT NULL,
    destination_country VARCHAR(2) NOT NULL,
    destination_region VARCHAR(50),
    destination_postal_code VARCHAR(20),
    
    -- Time components (in hours)
    warehouse_processing_hours INT DEFAULT 24,
    customs_clearance_hours INT DEFAULT 0, -- 0 for same country, 24 for EU, 48 for international
    transit_hours INT,
    
    -- Total estimate
    total_delivery_hours INT,
    total_delivery_days DECIMAL(4, 1),
    
    -- Method
    shipping_method VARCHAR(50),
    
    -- Metadata
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estimate_active BOOLEAN DEFAULT TRUE,
    
    -- Tenant
    tenant_id VARCHAR(50) NOT NULL,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1,
    
    CONSTRAINT unique_delivery_estimate UNIQUE (warehouse_id, destination_country, destination_postal_code, shipping_method)
);

CREATE INDEX idx_delivery_estimates_warehouse ON delivery_time_estimates(warehouse_id);
CREATE INDEX idx_delivery_estimates_destination ON delivery_time_estimates(destination_country, destination_region);

-- Customs clearance rules
CREATE TABLE IF NOT EXISTS customs_clearance_rules (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    origin_country VARCHAR(2) NOT NULL,
    destination_country VARCHAR(2) NOT NULL,
    
    -- Clearance time in hours
    clearance_hours INT NOT NULL,
    
    -- Rules
    is_same_country BOOLEAN DEFAULT FALSE,
    is_customs_union BOOLEAN DEFAULT FALSE,
    customs_union_code VARCHAR(20), -- EU, ASEAN, etc.
    
    description TEXT,
    
    rule_active BOOLEAN DEFAULT TRUE,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1
);

-- Insert default customs rules
INSERT INTO customs_clearance_rules (id, origin_country, destination_country, clearance_hours, is_same_country, is_customs_union, customs_union_code, description) VALUES
    -- Same country
    (100001, 'US', 'US', 0, TRUE, FALSE, NULL, 'Domestic shipping'),
    (100002, 'CN', 'CN', 0, TRUE, FALSE, NULL, 'Domestic shipping'),
    (100003, 'DE', 'DE', 0, TRUE, FALSE, NULL, 'Domestic shipping'),
    
    -- EU customs union
    (100004, 'DE', 'FR', 24, FALSE, TRUE, 'EU', 'EU customs union'),
    (100005, 'FR', 'IT', 24, FALSE, TRUE, 'EU', 'EU customs union'),
    (100006, 'NL', 'BE', 24, FALSE, TRUE, 'EU', 'EU customs union'),
    
    -- International
    (100007, 'CN', 'US', 48, FALSE, FALSE, NULL, 'International shipping'),
    (100008, 'US', 'CN', 48, FALSE, FALSE, NULL, 'International shipping'),
    (100009, 'DE', 'US', 48, FALSE, FALSE, NULL, 'International shipping')
ON CONFLICT DO NOTHING;
