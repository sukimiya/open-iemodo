-- Payment Service Schema
-- Schema: payment_{tenantId}

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    payment_no VARCHAR(32) UNIQUE NOT NULL,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    customer_id BIGINT NOT NULL,
    
    -- Amount and currency
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Payment channel
    channel VARCHAR(20) NOT NULL, -- STRIPE, PAYPAL, ALIPAY, WECHAT_PAY
    channel_sub_type VARCHAR(20), -- CARD, IDEAL, BANCONTACT, GIROPAY, EPS, P24, SOFORT
    
    -- Payment method details (tokenized, not storing full card numbers for PCI DSS)
    payment_method_id VARCHAR(100),
    payment_method_type VARCHAR(20),
    payment_method_last4 VARCHAR(4),
    payment_method_brand VARCHAR(20), -- visa, mastercard, amex
    
    -- Status
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED
    
    -- Third party transaction data
    third_party_txn_id VARCHAR(100),
    third_party_txn_data JSONB,
    
    -- Timestamps
    paid_at TIMESTAMP,
    expired_at TIMESTAMP NOT NULL, -- Payment expiration time (default 30 min)
    
    -- Refund info
    refunded_amount DECIMAL(19, 4) DEFAULT 0,
    refundable_amount DECIMAL(19, 4) GENERATED ALWAYS AS (amount - refunded_amount) STORED,
    
    -- Failure info
    failure_code VARCHAR(50),
    failure_message VARCHAR(500),
    
    -- Metadata
    description VARCHAR(255),
    metadata JSONB,
    
    -- Tenant info
    tenant_id VARCHAR(50) NOT NULL,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid         BOOLEAN         NOT NULL DEFAULT true
);

-- Indexes for payments
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_order_no ON payments(order_no);
CREATE INDEX idx_payments_customer_id ON payments(customer_id);
CREATE INDEX idx_payments_payment_status ON payments(payment_status);
CREATE INDEX idx_payments_channel ON payments(channel);
CREATE INDEX idx_payments_third_party_txn_id ON payments(third_party_txn_id);
CREATE INDEX idx_payments_tenant_id ON payments(tenant_id);
CREATE INDEX idx_payments_create_time ON payments(create_time);
CREATE INDEX idx_payments_expired_at ON payments(expired_at) WHERE payment_status = 'PENDING';

-- Refunds table
CREATE TABLE IF NOT EXISTS refunds (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    refund_no VARCHAR(32) UNIQUE NOT NULL,
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    order_id BIGINT NOT NULL,
    
    -- Amount
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Status
    refund_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, SUCCESS, FAILED
    
    -- Reason
    reason_type VARCHAR(30) NOT NULL, -- CUSTOMER_REQUEST, DUPLICATE, FRAUDULENT, ORDER_CANCELLED, PRODUCT_DEFECTIVE
    reason_description VARCHAR(500),
    
    -- Third party refund data
    third_party_refund_id VARCHAR(100),
    third_party_refund_data JSONB,
    
    -- Timestamps
    processed_at TIMESTAMP,
    
    -- Metadata
    metadata JSONB,
    
    -- Tenant info
    tenant_id VARCHAR(50) NOT NULL,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid         BOOLEAN         NOT NULL DEFAULT true
);

-- Indexes for refunds
CREATE INDEX idx_refunds_payment_id ON refunds(payment_id);
CREATE INDEX idx_refunds_order_id ON refunds(order_id);
CREATE INDEX idx_refunds_refund_status ON refunds(refund_status);
CREATE INDEX idx_refunds_tenant_id ON refunds(tenant_id);
CREATE INDEX idx_refunds_create_time ON refunds(create_time);

-- Payment audit log table (for compliance)
CREATE TABLE IF NOT EXISTS payment_audit_logs (
    id BIGINT PRIMARY KEY,  -- Snowflake ID
    payment_id BIGINT REFERENCES payments(id),
    refund_id BIGINT REFERENCES refunds(id),
    
    action VARCHAR(50) NOT NULL, -- CREATE, UPDATE, REFUND, WEBHOOK
    action_detail VARCHAR(255),
    
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    
    -- Request/response data (sanitized)
    request_data JSONB,
    response_data JSONB,
    
    -- IP and user agent
    ip_address INET,
    user_agent TEXT,
    
    -- User info
    user_id BIGINT,
    
    -- Tenant info
    tenant_id VARCHAR(50) NOT NULL,
    
    -- BaseEntity audit fields
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    is_valid         BOOLEAN         NOT NULL DEFAULT true
);

CREATE INDEX idx_payment_audit_logs_payment_id ON payment_audit_logs(payment_id);
CREATE INDEX idx_payment_audit_logs_tenant_id ON payment_audit_logs(tenant_id);
CREATE INDEX idx_payment_audit_logs_create_time ON payment_audit_logs(create_time);
