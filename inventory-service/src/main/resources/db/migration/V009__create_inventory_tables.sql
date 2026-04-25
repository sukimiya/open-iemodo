-- =============================================================
-- V1: Create inventory management tables
-- Schema: inventory_{tenantId}
-- =============================================================

-- ─── warehouses ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS warehouses (
    id                  BIGINT          PRIMARY KEY,  -- Snowflake ID
    warehouse_code      VARCHAR(50)     NOT NULL UNIQUE,
    name                VARCHAR(200)    NOT NULL,
    name_localized      JSONB,
    
    -- Address
    country_code        VARCHAR(2)      NOT NULL,
    region_code         VARCHAR(10),
    city                VARCHAR(100),
    district            VARCHAR(100),
    address             VARCHAR(500),
    postal_code         VARCHAR(20),
    
    -- Geolocation
    latitude            DECIMAL(10, 8),
    longitude           DECIMAL(11, 8),
    
    -- Warehouse type: STANDARD, EXPRESS, REFRIGERATED, BONDED
    warehouse_type      VARCHAR(20)     NOT NULL DEFAULT 'STANDARD',
    
    -- Service level
    service_level       VARCHAR(20)     DEFAULT 'STANDARD',  -- STANDARD, PREMIUM
    avg_process_hours   INTEGER         DEFAULT 24,          -- Average processing time
    
    -- Status
    warehouse_active    BOOLEAN         NOT NULL DEFAULT TRUE,
    is_default          BOOLEAN         DEFAULT FALSE,
    
    -- Capacity
    max_daily_orders    INTEGER,
    current_daily_orders INTEGER        DEFAULT 0,
    
    -- BaseEntity audit fields
    status              INTEGER         NOT NULL DEFAULT 1,
    create_by           BIGINT,
    create_time         TIMESTAMPTZ,
    update_by           BIGINT,
    update_time         TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true
);

CREATE INDEX IF NOT EXISTS idx_warehouses_country ON warehouses (country_code);
CREATE INDEX IF NOT EXISTS idx_warehouses_active ON warehouses (warehouse_active);
CREATE INDEX IF NOT EXISTS idx_warehouses_location ON warehouses (latitude, longitude) 
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- ─── inventory ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS inventory (
    id                  BIGINT          PRIMARY KEY,  -- Snowflake ID
    warehouse_id        BIGINT          NOT NULL REFERENCES warehouses(id),
    sku_id              BIGINT          NOT NULL,
    
    -- Stock quantities
    available_qty       INTEGER         NOT NULL DEFAULT 0,  -- Available for sale
    reserved_qty        INTEGER         NOT NULL DEFAULT 0,  -- Reserved for orders
    locked_qty          INTEGER         NOT NULL DEFAULT 0,  -- Locked (quality issues, etc.)
    inbound_qty         INTEGER         NOT NULL DEFAULT 0,  -- Incoming stock
    
    -- Computed columns
    sellable_qty        INTEGER         GENERATED ALWAYS AS (available_qty - reserved_qty) STORED,
    total_qty           INTEGER         GENERATED ALWAYS AS (available_qty + reserved_qty + locked_qty + inbound_qty) STORED,
    
    -- Stock thresholds
    min_stock_qty       INTEGER         DEFAULT 0,           -- Minimum stock level
    max_stock_qty       INTEGER,                             -- Maximum stock level
    reorder_point       INTEGER,                             -- Reorder threshold
    
    -- Optimistic locking
    version             INTEGER         NOT NULL DEFAULT 0,
    
    -- Last update tracking
    last_stock_in_at    TIMESTAMPTZ,
    last_stock_out_at   TIMESTAMPTZ,
    
    -- BaseEntity audit fields
    status              INTEGER         NOT NULL DEFAULT 1,
    create_by           BIGINT,
    create_time         TIMESTAMPTZ,
    update_by           BIGINT,
    update_time         TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true,
    
    UNIQUE(warehouse_id, sku_id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_warehouse ON inventory (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_sku ON inventory (sku_id);
CREATE INDEX IF NOT EXISTS idx_inventory_sellable ON inventory (sku_id, sellable_qty) WHERE sellable_qty > 0;
CREATE INDEX IF NOT EXISTS idx_inventory_low_stock ON inventory (sku_id, available_qty) WHERE available_qty <= reorder_point;

-- ─── inventory_transactions ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id                  BIGINT          PRIMARY KEY,  -- Snowflake ID
    warehouse_id        BIGINT          NOT NULL REFERENCES warehouses(id),
    sku_id              BIGINT          NOT NULL,
    
    -- Transaction details
    transaction_type    VARCHAR(20)     NOT NULL,            -- INBOUND, OUTBOUND, ADJUST, TRANSFER_IN, TRANSFER_OUT
    quantity            INTEGER         NOT NULL,            -- Positive for inbound, negative for outbound
    
    -- Before/After quantities
    before_available    INTEGER         NOT NULL,
    after_available     INTEGER         NOT NULL,
    before_reserved     INTEGER         NOT NULL,
    after_reserved      INTEGER         NOT NULL,
    
    -- Reference information
    reference_no        VARCHAR(100),                        -- Order No, Transfer No, etc.
    reference_type      VARCHAR(50),                         -- ORDER, TRANSFER, ADJUSTMENT
    
    -- Reason and notes
    reason              VARCHAR(200),
    notes               TEXT,
    
    -- BaseEntity audit fields
    status              INTEGER         NOT NULL DEFAULT 1,
    create_by           BIGINT,
    create_time         TIMESTAMPTZ,
    update_by           BIGINT,
    update_time         TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true
);

CREATE INDEX IF NOT EXISTS idx_transactions_warehouse ON inventory_transactions (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_transactions_sku ON inventory_transactions (sku_id);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON inventory_transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_reference ON inventory_transactions (reference_no);
CREATE INDEX IF NOT EXISTS idx_transactions_create_time ON inventory_transactions (create_time);

-- ─── stock_transfers ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS stock_transfers (
    id                  BIGINT          PRIMARY KEY,  -- Snowflake ID
    transfer_no         VARCHAR(100)    NOT NULL UNIQUE,
    
    -- Source and destination
    from_warehouse_id   BIGINT          NOT NULL REFERENCES warehouses(id),
    to_warehouse_id     BIGINT          NOT NULL REFERENCES warehouses(id),
    
    -- Status: PENDING, APPROVED, SHIPPED, RECEIVED, CANCELLED
    transfer_status     VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    
    -- Costs
    shipping_cost       DECIMAL(15,2),
    handling_cost       DECIMAL(15,2),
    total_cost          DECIMAL(15,2),
    
    -- Tracking
    carrier             VARCHAR(100),
    tracking_no         VARCHAR(200),
    
    -- Timestamps
    approved_at         TIMESTAMPTZ,
    shipped_at          TIMESTAMPTZ,
    received_at         TIMESTAMPTZ,
    
    -- Notes
    notes               TEXT,
    
    -- BaseEntity audit fields
    status              INTEGER         NOT NULL DEFAULT 1,
    create_by           BIGINT,
    create_time         TIMESTAMPTZ,
    update_by           BIGINT,
    update_time         TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true
);

CREATE INDEX IF NOT EXISTS idx_transfers_from ON stock_transfers (from_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_transfers_to ON stock_transfers (to_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_transfers_status ON stock_transfers (transfer_status);

-- ─── stock_transfer_items ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS stock_transfer_items (
    id                  BIGINT          PRIMARY KEY,  -- Snowflake ID
    transfer_id         BIGINT          NOT NULL REFERENCES stock_transfers(id) ON DELETE CASCADE,
    sku_id              BIGINT          NOT NULL,
    
    -- Quantities
    requested_qty       INTEGER         NOT NULL,
    shipped_qty         INTEGER,
    received_qty        INTEGER,
    
    -- Status per item
    item_status         VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    
    notes               TEXT,
    
    -- BaseEntity audit fields
    status              INTEGER         NOT NULL DEFAULT 1,
    create_by           BIGINT,
    create_time         TIMESTAMPTZ,
    update_by           BIGINT,
    update_time         TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true
);

CREATE INDEX IF NOT EXISTS idx_transfer_items_transfer ON stock_transfer_items (transfer_id);
CREATE INDEX IF NOT EXISTS idx_transfer_items_sku ON stock_transfer_items (sku_id);

-- ─── restock_recommendations ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS restock_recommendations (
    id                  BIGINT          PRIMARY KEY,  -- Snowflake ID
    warehouse_id        BIGINT          NOT NULL REFERENCES warehouses(id),
    sku_id              BIGINT          NOT NULL,
    
    -- Analysis
    analysis_type       VARCHAR(50)     NOT NULL,            -- DEMAND_FORECAST, LOW_STOCK, etc.
    lookback_days       INTEGER         NOT NULL,
    
    -- Recommendation
    current_stock       INTEGER         NOT NULL,
    avg_daily_sales     DECIMAL(10,2),
    suggested_qty       INTEGER         NOT NULL,
    recommendation_priority VARCHAR(20) DEFAULT 'MEDIUM',    -- HIGH, MEDIUM, LOW
    
    -- Source warehouse for transfer
    source_warehouse_id BIGINT          REFERENCES warehouses(id),
    estimated_cost      DECIMAL(15,2),
    
    -- Status: PENDING, APPROVED, REJECTED, EXECUTED
    recommendation_status VARCHAR(20)   DEFAULT 'PENDING',
    
    expires_at          TIMESTAMPTZ,
    
    -- BaseEntity audit fields
    status              INTEGER         NOT NULL DEFAULT 1,
    create_by           BIGINT,
    create_time         TIMESTAMPTZ,
    update_by           BIGINT,
    update_time         TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true
);

CREATE INDEX IF NOT EXISTS idx_restock_warehouse ON restock_recommendations (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_restock_sku ON restock_recommendations (sku_id);
CREATE INDEX IF NOT EXISTS idx_restock_status ON restock_recommendations (recommendation_status);

-- ─── Seed data ───────────────────────────────────────────────────────────────
INSERT INTO warehouses (id, warehouse_code, name, name_localized, country_code, city, warehouse_type, warehouse_active, is_default) VALUES
    (100001, 'WH-US-NYC', 'New York Warehouse', '{"en": "New York Warehouse"}', 'US', 'New York', 'STANDARD', TRUE, TRUE),
    (100002, 'WH-US-LAX', 'Los Angeles Warehouse', '{"en": "Los Angeles Warehouse"}', 'US', 'Los Angeles', 'EXPRESS', TRUE, FALSE),
    (100003, 'WH-CN-SHA', 'Shanghai Warehouse', '{"en": "Shanghai Warehouse", "zh": "上海仓库"}', 'CN', 'Shanghai', 'STANDARD', TRUE, FALSE),
    (100004, 'WH-EU-GER', 'Germany Warehouse', '{"en": "Germany Warehouse"}', 'DE', 'Berlin', 'STANDARD', TRUE, FALSE)
ON CONFLICT (warehouse_code) DO NOTHING;
