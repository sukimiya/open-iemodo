-- =============================================================
-- V020: Seed USAGE_ALERT notification templates
-- =============================================================

INSERT INTO notification_templates (type, channel, language, subject, body, active)
VALUES
    ('USAGE_ALERT', 'EMAIL', 'en',
     'Usage Alert: {{metric}} at {{threshold}}',
     'Your {{metric}} usage has reached {{threshold}} ({{currentUsage}}/{{limit}}). Please review your plan limits.',
     TRUE),
    ('USAGE_ALERT', 'EMAIL', 'zh-CN',
     '用量提醒：{{metric}} 已达到 {{threshold}}',
     '您的 {{metric}} 用量已达到 {{threshold}}（{{currentUsage}}/{{limit}}）。请查看您的套餐限制。',
     TRUE)
ON CONFLICT (type, channel, language) DO NOTHING;
