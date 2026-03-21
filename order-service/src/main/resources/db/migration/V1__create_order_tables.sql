-- =============================================================
-- V1: Create order tables in each tenant schema
-- =============================================================

-- ─── orders ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id              BIGSERIAL       PRIMARY KEY,
    order_no        VARCHAR(64)     NOT NULL,
    tenant_id       VARCHAR(50)     NOT NULL,       -- denormalized for cross-schema ops
    customer_id     BIGINT          NOT NULL,
    status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount    NUMERIC(15,2)   NOT NULL,
    currency        VARCHAR(10)     NOT NULL DEFAULT 'USD',
    shipping_name   VARCHAR(100),
    shipping_phone  VARCHAR(30),
    shipping_addr   VARCHAR(500),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT orders_order_no_unique UNIQUE (order_no)
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_id  ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status       ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_id    ON orders (tenant_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at   ON orders (created_at DESC);

-- ─── order_items ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    id          BIGSERIAL       PRIMARY KEY,
    order_id    BIGINT          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  BIGINT          NOT NULL,
    sku         VARCHAR(100)    NOT NULL,
    quantity    INTEGER         NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(15,2)   NOT NULL,
    subtotal    NUMERIC(15,2)   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);
