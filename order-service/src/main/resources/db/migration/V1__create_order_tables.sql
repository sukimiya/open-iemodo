-- =============================================================
-- V1: Create order tables in each tenant schema
-- =============================================================

-- ─── orders ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT          PRIMARY KEY,  -- Snowflake ID
    order_no        VARCHAR(64)     NOT NULL,
    tenant_id       VARCHAR(50)     NOT NULL,       -- denormalized for cross-schema ops
    customer_id     BIGINT          NOT NULL,
    order_status    VARCHAR(30)     NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount    NUMERIC(15,2)   NOT NULL,
    currency        VARCHAR(10)     NOT NULL DEFAULT 'USD',
    shipping_name   VARCHAR(100),
    shipping_phone  VARCHAR(30),
    shipping_addr   VARCHAR(500),
    
    -- BaseEntity audit fields
    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ,
    update_by       BIGINT,
    update_time     TIMESTAMPTZ,
    is_valid        INTEGER         NOT NULL DEFAULT 1,

    CONSTRAINT orders_order_no_unique UNIQUE (order_no)
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_id  ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_status ON orders (order_status);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_id    ON orders (tenant_id);
CREATE INDEX IF NOT EXISTS idx_orders_create_time  ON orders (create_time DESC);

-- ─── order_items ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    id          BIGINT          PRIMARY KEY,  -- Snowflake ID
    order_id    BIGINT          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  BIGINT          NOT NULL,
    sku         VARCHAR(100)    NOT NULL,
    quantity    INTEGER         NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(15,2)   NOT NULL,
    subtotal    NUMERIC(15,2)   NOT NULL,
    
    -- BaseEntity audit fields
    status      INTEGER         NOT NULL DEFAULT 1,
    create_by   BIGINT,
    create_time TIMESTAMPTZ,
    update_by   BIGINT,
    update_time TIMESTAMPTZ,
    is_valid    INTEGER         NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);
