-- =============================================================
-- Fulfillment Service Schema Initialization
-- Run this script in each tenant's fulfillment schema
-- =============================================================

-- Warehouses table
CREATE TABLE IF NOT EXISTS warehouses (
    id                  BIGSERIAL       PRIMARY KEY,
    warehouse_no        VARCHAR(50)     NOT NULL UNIQUE,
    name                VARCHAR(200)    NOT NULL,
    country_code        VARCHAR(2)      NOT NULL,
    province            VARCHAR(100),
    city                VARCHAR(100),
    address             VARCHAR(500),
    postal_code         VARCHAR(20),
    latitude            DECIMAL(10, 8),
    longitude           DECIMAL(11, 8),
    contact_name        VARCHAR(100),
    contact_phone       VARCHAR(30),
    email               VARCHAR(255),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    operating_hours     VARCHAR(100),
    capacity_sqm        INTEGER,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_warehouses_country ON warehouses (country_code);
CREATE INDEX IF NOT EXISTS idx_warehouses_active ON warehouses (is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_warehouses_location ON warehouses (latitude, longitude);

-- Warehouse capacities table
CREATE TABLE IF NOT EXISTS warehouse_capacities (
    id                  BIGSERIAL       PRIMARY KEY,
    warehouse_id        BIGINT          NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    sku                 VARCHAR(100)    NOT NULL,
    available_quantity  INTEGER         NOT NULL DEFAULT 0,
    reserved_quantity   INTEGER         NOT NULL DEFAULT 0,
    incoming_quantity   INTEGER         NOT NULL DEFAULT 0,
    max_capacity        INTEGER,
    reorder_point       INTEGER         DEFAULT 0,
    reorder_quantity    INTEGER         DEFAULT 0,
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT warehouse_capacities_unique UNIQUE (warehouse_id, sku),
    CONSTRAINT available_quantity_check CHECK (available_quantity >= 0),
    CONSTRAINT reserved_quantity_check CHECK (reserved_quantity >= 0),
    CONSTRAINT incoming_quantity_check CHECK (incoming_quantity >= 0)
);

CREATE INDEX IF NOT EXISTS idx_warehouse_capacities_warehouse ON warehouse_capacities (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_capacities_sku ON warehouse_capacities (sku);

-- Delivery routes table
CREATE TABLE IF NOT EXISTS delivery_routes (
    id                      BIGSERIAL       PRIMARY KEY,
    origin_warehouse_id     BIGINT          NOT NULL REFERENCES warehouses(id),
    destination_country     VARCHAR(2)      NOT NULL,
    destination_postal_prefix VARCHAR(10),
    transit_days_min        INTEGER         NOT NULL DEFAULT 1,
    transit_days_max        INTEGER         NOT NULL DEFAULT 3,
    shipping_cost_base      DECIMAL(10, 2)  NOT NULL DEFAULT 0,
    shipping_cost_per_kg    DECIMAL(10, 2)  NOT NULL DEFAULT 0,
    carrier                 VARCHAR(100),
    service_level           VARCHAR(50),
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT delivery_routes_unique UNIQUE (origin_warehouse_id, destination_country, destination_postal_prefix)
);

CREATE INDEX IF NOT EXISTS idx_delivery_routes_origin ON delivery_routes (origin_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_delivery_routes_dest ON delivery_routes (destination_country);

-- Customs clearance rules table
CREATE TABLE IF NOT EXISTS customs_clearance_rules (
    id                  BIGSERIAL       PRIMARY KEY,
    origin_country      VARCHAR(2),
    destination_country VARCHAR(2)      NOT NULL,
    clearance_hours     INTEGER         NOT NULL DEFAULT 48,
    is_same_country     BOOLEAN         NOT NULL DEFAULT FALSE,
    is_customs_union    BOOLEAN         NOT NULL DEFAULT FALSE,
    customs_union_code  VARCHAR(10),
    description         VARCHAR(500),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    effective_from      DATE,
    effective_to        DATE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_customs_rules_countries ON customs_clearance_rules (origin_country, destination_country);
CREATE INDEX IF NOT EXISTS idx_customs_rules_active ON customs_clearance_rules (is_active) WHERE is_active = TRUE;

-- Allocation logs table
CREATE TABLE IF NOT EXISTS allocation_logs (
    id                  BIGSERIAL       PRIMARY KEY,
    allocation_no       VARCHAR(50)     NOT NULL UNIQUE,
    order_id            VARCHAR(100)    NOT NULL,
    warehouse_id        BIGINT          REFERENCES warehouses(id),
    destination_country VARCHAR(2)      NOT NULL,
    destination_postal  VARCHAR(20),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    score               DECIMAL(5, 4),
    allocated_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT allocation_logs_status_check CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REALLOCATED'))
);

CREATE INDEX IF NOT EXISTS idx_allocation_logs_order ON allocation_logs (order_id);
CREATE INDEX IF NOT EXISTS idx_allocation_logs_status ON allocation_logs (status);

-- Allocation items table
CREATE TABLE IF NOT EXISTS allocation_items (
    id                  BIGSERIAL       PRIMARY KEY,
    allocation_id       BIGINT          NOT NULL REFERENCES allocation_logs(id) ON DELETE CASCADE,
    sku                 VARCHAR(100)    NOT NULL,
    quantity            INTEGER         NOT NULL,
    allocated_quantity  INTEGER         NOT NULL DEFAULT 0,
    
    CONSTRAINT allocation_items_quantity_check CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_allocation_items_allocation ON allocation_items (allocation_id);

-- Stock transfer recommendations table
CREATE TABLE IF NOT EXISTS stock_transfer_recommendations (
    id                  BIGSERIAL       PRIMARY KEY,
    recommendation_no   VARCHAR(50)     NOT NULL UNIQUE,
    from_warehouse_id   BIGINT          NOT NULL REFERENCES warehouses(id),
    to_warehouse_id     BIGINT          NOT NULL REFERENCES warehouses(id),
    sku                 VARCHAR(100)    NOT NULL,
    recommended_quantity INTEGER        NOT NULL,
    reason              VARCHAR(500),
    priority            INTEGER         NOT NULL DEFAULT 3,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    analysis_type       VARCHAR(20)     NOT NULL,
    confidence_score    DECIMAL(5, 4),
    executed_quantity   INTEGER         DEFAULT 0,
    executed_at         TIMESTAMPTZ,
    executed_by         BIGINT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT stock_transfer_status_check CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXECUTED', 'CANCELLED')),
    CONSTRAINT stock_transfer_type_check CHECK (analysis_type IN ('SKU', 'WAREHOUSE', 'REGIONAL')),
    CONSTRAINT different_warehouses_check CHECK (from_warehouse_id != to_warehouse_id)
);

CREATE INDEX IF NOT EXISTS idx_stock_transfer_from ON stock_transfer_recommendations (from_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_stock_transfer_to ON stock_transfer_recommendations (to_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_stock_transfer_status ON stock_transfer_recommendations (status);
CREATE INDEX IF NOT EXISTS idx_stock_transfer_sku ON stock_transfer_recommendations (sku);

-- Insert sample warehouse data
INSERT INTO warehouses (warehouse_no, name, country_code, province, city, address, postal_code, latitude, longitude, is_active)
VALUES 
    ('WH-CN-BJ-001', 'Beijing Warehouse', 'CN', 'Beijing', 'Beijing', 'Chaoyang District', '100000', 39.9042, 116.4074, TRUE),
    ('WH-CN-SH-001', 'Shanghai Warehouse', 'CN', 'Shanghai', 'Shanghai', 'Pudong New Area', '200000', 31.2304, 121.4737, TRUE),
    ('WH-US-LA-001', 'Los Angeles Warehouse', 'US', 'California', 'Los Angeles', '123 Commerce St', '90001', 34.0522, -118.2437, TRUE),
    ('WH-US-NY-001', 'New York Warehouse', 'US', 'New York', 'New York', '456 Trade Ave', '10001', 40.7128, -74.0060, TRUE),
    ('WH-DE-BE-001', 'Berlin Warehouse', 'DE', 'Berlin', 'Berlin', '789 Logistics Blvd', '10115', 52.5200, 13.4050, TRUE),
    ('WH-JP-TK-001', 'Tokyo Warehouse', 'JP', 'Tokyo', 'Tokyo', '321 Supply Rd', '100-0001', 35.6762, 139.6503, TRUE)
ON CONFLICT (warehouse_no) DO NOTHING;

-- Insert sample customs clearance rules
INSERT INTO customs_clearance_rules (origin_country, destination_country, clearance_hours, is_same_country, is_customs_union, customs_union_code, description, is_active)
VALUES
    ('CN', 'CN', 0, TRUE, FALSE, NULL, 'Domestic shipping - no customs', TRUE),
    ('US', 'US', 0, TRUE, FALSE, NULL, 'Domestic shipping - no customs', TRUE),
    ('DE', 'DE', 0, TRUE, FALSE, NULL, 'Domestic shipping - no customs', TRUE),
    ('DE', 'FR', 0, FALSE, TRUE, 'EU', 'EU customs union', TRUE),
    ('DE', 'IT', 0, FALSE, TRUE, 'EU', 'EU customs union', TRUE),
    ('CN', 'US', 48, FALSE, FALSE, NULL, 'China to US international', TRUE),
    ('US', 'CN', 48, FALSE, FALSE, NULL, 'US to China international', TRUE),
    ('CN', 'DE', 72, FALSE, FALSE, NULL, 'China to EU international', TRUE),
    ('US', 'DE', 48, FALSE, FALSE, NULL, 'US to EU international', TRUE)
ON CONFLICT DO NOTHING;
