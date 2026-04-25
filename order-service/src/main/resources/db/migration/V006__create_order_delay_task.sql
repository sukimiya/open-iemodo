-- V3: Payment timeout auto-close — delay task table
CREATE TABLE IF NOT EXISTS order_delay_task (
    id           BIGINT        PRIMARY KEY,  -- Snowflake ID
    order_id     BIGINT        NOT NULL,
    tenant_id    VARCHAR(50)   NOT NULL,
    task_type    VARCHAR(50)   NOT NULL,
    execute_time TIMESTAMPTZ   NOT NULL,
    task_status  VARCHAR(20)   NOT NULL DEFAULT 'PENDING',

    -- BaseEntity audit fields
    status       INTEGER       NOT NULL DEFAULT 1,
    create_by    BIGINT,
    create_time  TIMESTAMPTZ,
    update_by    BIGINT,
    update_time  TIMESTAMPTZ,
    is_valid         BOOLEAN         NOT NULL DEFAULT true
);

CREATE INDEX IF NOT EXISTS idx_delay_task_execute_status ON order_delay_task (execute_time, task_status);
CREATE INDEX IF NOT EXISTS idx_delay_task_order_id       ON order_delay_task (order_id);
