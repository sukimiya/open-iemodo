-- Pricing Service Schema
-- Schema: pricing (shared schema for functional module)

-- Currencies table
CREATE TABLE IF NOT EXISTS currencies (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    code VARCHAR(3) UNIQUE NOT NULL,       -- ISO 4217: USD, EUR, CNY, etc.
    name VARCHAR(50) NOT NULL,             -- Currency name
    symbol VARCHAR(10),                    -- Currency symbol: $, €, ¥
    decimal_places INT DEFAULT 2,          -- Number of decimal places
    is_active BOOLEAN DEFAULT TRUE,        -- Whether currency is active
    is_base_currency BOOLEAN DEFAULT FALSE, -- Base currency for conversions (USD)
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1
);

-- Insert common currencies
INSERT INTO currencies (id, code, name, symbol, decimal_places, is_active, is_base_currency) VALUES
    (100001, 'USD', 'US Dollar', '$', 2, TRUE, TRUE),
    (100002, 'EUR', 'Euro', '€', 2, TRUE, FALSE),
    (100003, 'GBP', 'British Pound', '£', 2, TRUE, FALSE),
    (100004, 'CNY', 'Chinese Yuan', '¥', 2, TRUE, FALSE),
    (100005, 'JPY', 'Japanese Yen', '¥', 0, TRUE, FALSE),
    (100006, 'AUD', 'Australian Dollar', 'A$', 2, TRUE, FALSE),
    (100007, 'CAD', 'Canadian Dollar', 'C$', 2, TRUE, FALSE),
    (100008, 'CHF', 'Swiss Franc', 'Fr', 2, TRUE, FALSE),
    (100009, 'HKD', 'Hong Kong Dollar', 'HK$', 2, TRUE, FALSE),
    (100010, 'SGD', 'Singapore Dollar', 'S$', 2, TRUE, FALSE),
    (100011, 'INR', 'Indian Rupee', '₹', 2, TRUE, FALSE),
    (100012, 'KRW', 'South Korean Won', '₩', 0, TRUE, FALSE),
    (100013, 'BRL', 'Brazilian Real', 'R$', 2, TRUE, FALSE),
    (100014, 'RUB', 'Russian Ruble', '₽', 2, TRUE, FALSE),
    (100015, 'MXN', 'Mexican Peso', '$', 2, TRUE, FALSE),
    (100016, 'ZAR', 'South African Rand', 'R', 2, TRUE, FALSE),
    (100017, 'SEK', 'Swedish Krona', 'kr', 2, TRUE, FALSE),
    (100018, 'NOK', 'Norwegian Krone', 'kr', 2, TRUE, FALSE),
    (100019, 'NZD', 'New Zealand Dollar', 'NZ$', 2, TRUE, FALSE),
    (100020, 'PLN', 'Polish Zloty', 'zł', 2, TRUE, FALSE)
ON CONFLICT (code) DO NOTHING;

-- Exchange rates table (historical rates)
CREATE TABLE IF NOT EXISTS exchange_rates (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(19, 10) NOT NULL,         -- Exchange rate (from -> to)
    inverse_rate DECIMAL(19, 10),          -- 1/rate for quick lookup
    source VARCHAR(50) DEFAULT 'API',      -- Source: API, MANUAL, CALCULATED
    api_provider VARCHAR(50),              -- fixer, exchangerate-api, etc.
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1,
    
    CONSTRAINT unique_rate_per_time UNIQUE (from_currency, to_currency, recorded_at),
    CONSTRAINT positive_rate CHECK (rate > 0)
);

-- Index for faster queries
CREATE INDEX idx_exchange_rates_from_to ON exchange_rates(from_currency, to_currency);
CREATE INDEX idx_exchange_rates_recorded_at ON exchange_rates(recorded_at);
CREATE INDEX idx_exchange_rates_lookup ON exchange_rates(from_currency, to_currency, recorded_at DESC);

-- Regional pricing configuration
CREATE TABLE IF NOT EXISTS regional_pricing_config (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    country_code VARCHAR(2) NOT NULL,      -- ISO 3166-1 alpha-2: US, CN, etc.
    sku VARCHAR(100),                      -- NULL = default for country
    currency_code VARCHAR(3) NOT NULL,
    
    -- Pricing multipliers
    markup_multiplier DECIMAL(5, 4) DEFAULT 1.0000,  -- e.g., 1.15 = 15% markup
    discount_multiplier DECIMAL(5, 4) DEFAULT 1.0000, -- e.g., 0.90 = 10% discount
    
    -- Price constraints
    min_price DECIMAL(19, 4),
    max_price DECIMAL(19, 4),
    
    -- Strategy
    pricing_strategy VARCHAR(20) DEFAULT 'STANDARD', -- STANDARD, DYNAMIC, COMPETITIVE
    
    -- Effective period
    effective_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    effective_to TIMESTAMP,
    
    -- Metadata
    config_active BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 0,                -- Higher = more specific config
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1,
    
    CONSTRAINT unique_region_sku UNIQUE (country_code, sku),
    CONSTRAINT positive_markup CHECK (markup_multiplier >= 0),
    CONSTRAINT fk_currency FOREIGN KEY (currency_code) REFERENCES currencies(code)
);

CREATE INDEX idx_regional_pricing_country ON regional_pricing_config(country_code);
CREATE INDEX idx_regional_pricing_sku ON regional_pricing_config(sku);
CREATE INDEX idx_regional_pricing_active ON regional_pricing_config(config_active, effective_from, effective_to);

-- Quantity discount tiers
CREATE TABLE IF NOT EXISTS quantity_discount_tiers (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    sku VARCHAR(100),                      -- NULL = apply to all SKUs
    country_code VARCHAR(2),               -- NULL = apply to all countries
    min_quantity INT NOT NULL,
    max_quantity INT,                      -- NULL = no upper limit
    discount_percent DECIMAL(5, 2) NOT NULL, -- e.g., 10.00 = 10% off
    tier_active BOOLEAN DEFAULT TRUE,
    effective_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    effective_to TIMESTAMP,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1,
    
    CONSTRAINT positive_quantity CHECK (min_quantity > 0),
    CONSTRAINT valid_discount CHECK (discount_percent >= 0 AND discount_percent <= 100)
);

CREATE INDEX idx_qty_discount_sku ON quantity_discount_tiers(sku);
CREATE INDEX idx_qty_discount_country ON quantity_discount_tiers(country_code);

-- Customer segment pricing
CREATE TABLE IF NOT EXISTS segment_pricing (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    segment_code VARCHAR(50) NOT NULL,     -- VIP, WHOLESALE, STUDENT, etc.
    sku VARCHAR(100),                      -- NULL = apply to all
    discount_percent DECIMAL(5, 2) NOT NULL,
    segment_active BOOLEAN DEFAULT TRUE,
    effective_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    effective_to TIMESTAMP,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_segment_pricing_segment ON segment_pricing(segment_code);
CREATE INDEX idx_segment_pricing_sku ON segment_pricing(sku);
