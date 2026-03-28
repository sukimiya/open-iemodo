-- =============================================================
-- V1: Create product catalog tables
-- Schema: product_{tenantId}
-- =============================================================

-- ─── brands ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS brands (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    name_localized  JSONB,                          -- {"en": "Apple", "zh": "苹果"}
    logo_url        VARCHAR(500),
    website         VARCHAR(500),
    description     TEXT,
    country_code    VARCHAR(2),                     -- Brand origin country
    sort_order      INTEGER         DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_brands_active ON brands (is_active);

-- ─── categories ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS categories (
    id              BIGSERIAL       PRIMARY KEY,
    parent_id       BIGINT          REFERENCES categories(id),
    name            VARCHAR(200)    NOT NULL,
    name_localized  JSONB,
    description     TEXT,
    description_localized JSONB,
    image_url       VARCHAR(500),
    level           INTEGER         NOT NULL DEFAULT 1,  -- 1:一级, 2:二级, 3:三级
    path            VARCHAR(500),                       -- 路径如: /1/5/23
    sort_order      INTEGER         DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    seo_keywords    VARCHAR(500),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_categories_parent ON categories (parent_id);
CREATE INDEX IF NOT EXISTS idx_categories_level ON categories (level);
CREATE INDEX IF NOT EXISTS idx_categories_active ON categories (is_active);
CREATE INDEX IF NOT EXISTS idx_categories_path ON categories (path);

-- ─── products ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    id                  BIGSERIAL       PRIMARY KEY,
    product_code        VARCHAR(100)    NOT NULL UNIQUE,  -- 商品编码
    spu_code            VARCHAR(100),                       -- SPU编码
    title               VARCHAR(500)    NOT NULL,
    title_localized     JSONB,
    description         TEXT,
    description_localized JSONB,
    
    -- 分类与品牌
    category_id         BIGINT          NOT NULL REFERENCES categories(id),
    brand_id            BIGINT          REFERENCES brands(id),
    
    -- 物理属性
    weight_g            INTEGER,                            -- 重量(克)
    length_cm           DECIMAL(10,2),                      -- 长(cm)
    width_cm            DECIMAL(10,2),                      -- 宽(cm)
    height_cm           DECIMAL(10,2),                      -- 高(cm)
    
    -- 基础价格
    base_price          DECIMAL(15,2)   NOT NULL,
    cost_price          DECIMAL(15,2),                      -- 成本价
    market_price        DECIMAL(15,2),                      -- 市场价
    
    -- 状态与特性
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',  -- DRAFT/ACTIVE/ARCHIVED
    is_featured         BOOLEAN         NOT NULL DEFAULT FALSE,    -- 是否精选
    is_new_arrival      BOOLEAN         NOT NULL DEFAULT FALSE,    -- 是否新品
    
    -- 海关与合规
    hs_code             VARCHAR(50),                        -- 海关编码
    origin_country      VARCHAR(2),                         -- 原产国
    certifications      JSONB,                              -- 认证信息
    
    -- 媒体
    main_image          VARCHAR(500),
    video_url           VARCHAR(500),
    
    -- 属性与搜索
    attributes          JSONB,                              -- 商品属性
    search_keywords     VARCHAR(1000),                      -- 搜索关键词
    search_vector       TSVECTOR,                           -- 全文搜索向量
    
    -- 统计
    view_count          INTEGER         DEFAULT 0,
    sale_count          INTEGER         DEFAULT 0,
    
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ                         -- 软删除
);

CREATE INDEX IF NOT EXISTS idx_products_status ON products (status);
CREATE INDEX IF NOT EXISTS idx_products_category ON products (category_id);
CREATE INDEX IF NOT EXISTS idx_products_brand ON products (brand_id);
CREATE INDEX IF NOT EXISTS idx_products_featured ON products (is_featured) WHERE is_featured = TRUE;
CREATE INDEX IF NOT EXISTS idx_products_new ON products (is_new_arrival) WHERE is_new_arrival = TRUE;
CREATE INDEX IF NOT EXISTS idx_products_deleted ON products (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_products_search ON products USING GIN(search_vector);

-- ─── skus ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS skus (
    id                  BIGSERIAL       PRIMARY KEY,
    product_id          BIGINT          NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku_code            VARCHAR(100)    NOT NULL UNIQUE,
    barcode             VARCHAR(100),                       -- 条形码
    
    -- 属性
    attributes          JSONB           NOT NULL,           -- {"color": "red", "size": "XL"}
    attribute_hash      VARCHAR(64)     NOT NULL,           -- 属性组合哈希
    
    -- 图片
    image_url           VARCHAR(500),
    
    -- 价格
    price               DECIMAL(15,2)   NOT NULL,
    cost_price          DECIMAL(15,2),
    
    -- 库存
    stock_quantity      INTEGER         NOT NULL DEFAULT 0,
    reserved_quantity   INTEGER         NOT NULL DEFAULT 0, -- 预留库存
    
    -- 国家可用性
    available_in_countries  VARCHAR(2)[],                   -- 白名单
    banned_in_countries     VARCHAR(2)[],                   -- 黑名单
    
    -- 状态
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/OUT_OF_STOCK/DISABLED
    
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_skus_product ON skus (product_id);
CREATE INDEX IF NOT EXISTS idx_skus_status ON skus (status);
CREATE INDEX IF NOT EXISTS idx_skus_hash ON skus (attribute_hash);
CREATE INDEX IF NOT EXISTS idx_skus_deleted ON skus (deleted_at) WHERE deleted_at IS NULL;

-- ─── product_media ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_media (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    media_type      VARCHAR(20)     NOT NULL,           -- IMAGE/VIDEO
    url             VARCHAR(500)    NOT NULL,
    thumbnail_url   VARCHAR(500),
    sort_order      INTEGER         DEFAULT 0,
    is_main         BOOLEAN         DEFAULT FALSE,      -- 是否主图
    alt_text        VARCHAR(200),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_media_product ON product_media (product_id);
CREATE INDEX IF NOT EXISTS idx_media_main ON product_media (product_id, is_main) WHERE is_main = TRUE;

-- ─── product_regional_prices ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_regional_prices (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku_id          BIGINT          REFERENCES skus(id) ON DELETE CASCADE,
    country_code    VARCHAR(2)      NOT NULL,
    currency        VARCHAR(3)      NOT NULL,
    price           DECIMAL(15,2)   NOT NULL,
    compare_price   DECIMAL(15,2),                      -- 划线价
    is_active       BOOLEAN         DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    UNIQUE(product_id, sku_id, country_code)
);

CREATE INDEX IF NOT EXISTS idx_regional_prices_product ON product_regional_prices (product_id);
CREATE INDEX IF NOT EXISTS idx_regional_prices_sku ON product_regional_prices (sku_id);
CREATE INDEX IF NOT EXISTS idx_regional_prices_country ON product_regional_prices (country_code);

-- ─── product_country_visibility ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_country_visibility (
    id                  BIGSERIAL       PRIMARY KEY,
    product_id          BIGINT          NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    country_code        VARCHAR(2)      NOT NULL,
    is_visible          BOOLEAN         NOT NULL DEFAULT TRUE,
    is_purchasable      BOOLEAN         NOT NULL DEFAULT TRUE,  -- 是否可购买
    restriction_reason  VARCHAR(200),                           -- 限制原因
    required_certifications VARCHAR(100)[],                     -- 需要的认证
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    UNIQUE(product_id, country_code)
);

CREATE INDEX IF NOT EXISTS idx_visibility_product ON product_country_visibility (product_id);
CREATE INDEX IF NOT EXISTS idx_visibility_country ON product_country_visibility (country_code);
CREATE INDEX IF NOT EXISTS idx_visibility_visible ON product_country_visibility (country_code, is_visible);

-- ─── Seed data ───────────────────────────────────────────────────────────────
INSERT INTO brands (name, name_localized, country_code, is_active) VALUES
    ('Apple', '{"en": "Apple", "zh": "苹果"}', 'US', TRUE),
    ('Samsung', '{"en": "Samsung", "zh": "三星"}', 'KR', TRUE),
    ('Sony', '{"en": "Sony", "zh": "索尼"}', 'JP', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO categories (name, name_localized, level, path, sort_order, is_active) VALUES
    ('Electronics', '{"en": "Electronics", "zh": "电子产品"}', 1, '/1', 1, TRUE),
    ('Clothing', '{"en": "Clothing", "zh": "服装"}', 1, '/2', 2, TRUE),
    ('Home & Garden', '{"en": "Home & Garden", "zh": "家居园艺"}', 1, '/3', 3, TRUE)
ON CONFLICT DO NOTHING;
