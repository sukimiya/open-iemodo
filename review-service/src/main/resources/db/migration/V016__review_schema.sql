-- =============================================================
-- V1: Review and rating tables
-- =============================================================

-- ─── reviews ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    id              BIGINT          PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL,
    product_id      BIGINT          NOT NULL,
    sku_id          BIGINT,
    order_id        BIGINT          NOT NULL,
    order_item_id   BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,

    rating          SMALLINT        NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title           VARCHAR(200),
    content         TEXT,
    media_urls      TEXT,           -- comma-separated file-service URLs

    review_status   VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    helpful_count   INTEGER         NOT NULL DEFAULT 0,
    approved_at     TIMESTAMPTZ,

    -- BaseEntity
    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ,
    update_by       BIGINT,
    update_time     TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true,

    CONSTRAINT reviews_status_check
        CHECK (review_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    -- One review per purchased item per user
    CONSTRAINT reviews_order_item_user_unique UNIQUE (order_item_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_reviews_product_id     ON reviews (product_id, review_status);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id        ON reviews (user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_status         ON reviews (review_status);
CREATE INDEX IF NOT EXISTS idx_reviews_create_time    ON reviews (create_time DESC);

-- ─── review_replies ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS review_replies (
    id              BIGINT          PRIMARY KEY,
    review_id       BIGINT          NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    replier_id      BIGINT          NOT NULL,
    replier_type    VARCHAR(20)     NOT NULL,   -- MERCHANT | PLATFORM
    content         TEXT            NOT NULL,

    -- BaseEntity
    status          INTEGER         NOT NULL DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMPTZ,
    update_by       BIGINT,
    update_time     TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true,

    CONSTRAINT review_replies_type_check
        CHECK (replier_type IN ('MERCHANT', 'PLATFORM'))
);

CREATE INDEX IF NOT EXISTS idx_review_replies_review_id ON review_replies (review_id);

-- ─── product_rating_summary ───────────────────────────────────────────────────
-- Denormalized aggregate — updated each time a review is approved or rejected.
CREATE TABLE IF NOT EXISTS product_rating_summary (
    product_id      BIGINT          PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL,
    avg_rating      DECIMAL(3,2)    NOT NULL DEFAULT 0,
    total_reviews   INTEGER         NOT NULL DEFAULT 0,
    five_star       INTEGER         NOT NULL DEFAULT 0,
    four_star       INTEGER         NOT NULL DEFAULT 0,
    three_star      INTEGER         NOT NULL DEFAULT 0,
    two_star        INTEGER         NOT NULL DEFAULT 0,
    one_star        INTEGER         NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_product_rating_summary_tenant ON product_rating_summary (tenant_id);
