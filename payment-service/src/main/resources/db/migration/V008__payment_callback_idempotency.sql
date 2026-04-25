-- V2: Webhook callback idempotency log
-- Ensures each Stripe event_id is processed exactly once, even under duplicate delivery.
CREATE TABLE IF NOT EXISTS payment_callback_log (
    id                BIGINT        PRIMARY KEY,  -- Snowflake ID
    event_id          VARCHAR(100)  NOT NULL,
    event_type        VARCHAR(50)   NOT NULL,
    payment_intent_id VARCHAR(100),

    -- BaseEntity audit fields
    status            INTEGER       NOT NULL DEFAULT 1,
    create_by         BIGINT,
    create_time       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT,
    update_time       TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true,

    CONSTRAINT uq_callback_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_callback_log_intent ON payment_callback_log (payment_intent_id);
