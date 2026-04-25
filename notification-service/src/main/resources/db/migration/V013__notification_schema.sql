-- ─────────────────────────────────────────────────────────────────────────────
-- notification_templates
--   Stores subject + body templates per (type, channel, language).
--   Variables use {{key}} syntax — resolved at render time.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notification_templates (
    id          BIGSERIAL PRIMARY KEY,
    type        VARCHAR(64)   NOT NULL,
    channel     VARCHAR(16)   NOT NULL,  -- EMAIL | SMS | PUSH
    language    VARCHAR(16)   NOT NULL,  -- BCP-47: zh-CN | en | ja ...
    subject     VARCHAR(255),
    body        TEXT          NOT NULL,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    create_time TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_template UNIQUE (type, channel, language)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- notification_records
--   Persistent audit log of every send attempt.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notification_records (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     VARCHAR(64),
    user_id       BIGINT,
    channel       VARCHAR(16)  NOT NULL,
    type          VARCHAR(64)  NOT NULL,
    recipient     VARCHAR(255) NOT NULL,
    subject       VARCHAR(255),
    body          TEXT,
    send_status   VARCHAR(16)  NOT NULL DEFAULT 'PENDING',  -- PENDING | SENT | FAILED
    retry_count   INT          NOT NULL DEFAULT 0,
    error_message TEXT,
    sent_at       TIMESTAMPTZ,
    create_time   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    update_time   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_nr_user_id     ON notification_records (user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_nr_send_status ON notification_records (send_status, create_time ASC);

-- ─────────────────────────────────────────────────────────────────────────────
-- Seed templates (zh-CN + en)
-- Variables: {{userName}}, {{orderNo}}, {{totalAmount}}, {{currency}},
--            {{rmaNo}}, {{rmaType}}, {{reviewTitle}}, {{couponCode}},
--            {{expiresAt}}, {{productName}}, {{newPrice}}
-- ─────────────────────────────────────────────────────────────────────────────

-- USER_REGISTERED ─────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('USER_REGISTERED', 'EMAIL', 'zh-CN',
 '欢迎加入 iemodo！',
 '<h2>你好，{{userName}}！</h2><p>感谢您注册 iemodo，开始您的跨境购物之旅吧。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('USER_REGISTERED', 'EMAIL', 'en',
 'Welcome to iemodo!',
 '<h2>Hello, {{userName}}!</h2><p>Thank you for joining iemodo. Start your cross-border shopping journey today.</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- PASSWORD_RESET ──────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('PASSWORD_RESET', 'EMAIL', 'zh-CN',
 '重置您的 iemodo 密码',
 '<p>您好，{{userName}}，</p><p>请点击以下链接在 30 分钟内完成密码重置：</p><p><a href="{{resetLink}}">重置密码</a></p><p>如非本人操作，请忽略此邮件。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('PASSWORD_RESET', 'EMAIL', 'en',
 'Reset your iemodo password',
 '<p>Hi {{userName}},</p><p>Click the link below to reset your password (valid for 30 minutes):</p><p><a href="{{resetLink}}">Reset password</a></p><p>If you did not request this, please ignore this email.</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- ORDER_CREATED ───────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('ORDER_CREATED', 'EMAIL', 'zh-CN',
 '您的订单 {{orderNo}} 已创建',
 '<p>您好，{{userName}}，</p><p>您的订单 <strong>{{orderNo}}</strong> 已成功创建，合计 {{currency}} {{totalAmount}}。</p><p>我们将在支付确认后尽快为您发货。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('ORDER_CREATED', 'EMAIL', 'en',
 'Order {{orderNo}} confirmed',
 '<p>Hi {{userName}},</p><p>Your order <strong>{{orderNo}}</strong> has been placed. Total: {{currency}} {{totalAmount}}.</p><p>We will ship it once payment is confirmed.</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- ORDER_CANCELLED ─────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('ORDER_CANCELLED', 'EMAIL', 'zh-CN',
 '您的订单 {{orderNo}} 已取消',
 '<p>您好，{{userName}}，</p><p>订单 <strong>{{orderNo}}</strong> 已取消。如已付款，退款将在 3-5 个工作日内到账。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('ORDER_CANCELLED', 'EMAIL', 'en',
 'Order {{orderNo}} cancelled',
 '<p>Hi {{userName}},</p><p>Order <strong>{{orderNo}}</strong> has been cancelled. If payment was made, the refund will arrive within 3-5 business days.</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- PAYMENT_SUCCESS ─────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('PAYMENT_SUCCESS', 'EMAIL', 'zh-CN',
 '支付成功 — 订单 {{orderNo}}',
 '<p>您好，{{userName}}，</p><p>您对订单 <strong>{{orderNo}}</strong> 的付款（{{currency}} {{totalAmount}}）已成功收到，我们即将为您备货发货。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('PAYMENT_SUCCESS', 'EMAIL', 'en',
 'Payment confirmed — Order {{orderNo}}',
 '<p>Hi {{userName}},</p><p>We have received your payment of {{currency}} {{totalAmount}} for order <strong>{{orderNo}}</strong>. Your items will be shipped soon.</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- PAYMENT_FAILED ──────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('PAYMENT_FAILED', 'EMAIL', 'zh-CN',
 '支付失败 — 订单 {{orderNo}}',
 '<p>您好，{{userName}}，</p><p>订单 <strong>{{orderNo}}</strong> 的支付未能成功，请检查您的支付方式后重试。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('PAYMENT_FAILED', 'EMAIL', 'en',
 'Payment failed — Order {{orderNo}}',
 '<p>Hi {{userName}},</p><p>Payment for order <strong>{{orderNo}}</strong> was unsuccessful. Please check your payment method and try again.</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- REFUND_SUCCESS ──────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('REFUND_SUCCESS', 'EMAIL', 'zh-CN',
 '退款已处理 — 订单 {{orderNo}}',
 '<p>您好，{{userName}}，</p><p>订单 <strong>{{orderNo}}</strong> 的退款（{{currency}} {{totalAmount}}）已处理完毕，预计 3-5 个工作日到账。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('REFUND_SUCCESS', 'EMAIL', 'en',
 'Refund processed — Order {{orderNo}}',
 '<p>Hi {{userName}},</p><p>Your refund of {{currency}} {{totalAmount}} for order <strong>{{orderNo}}</strong> has been processed and will arrive within 3-5 business days.</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- ORDER_SHIPPED ───────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('ORDER_SHIPPED', 'EMAIL', 'zh-CN',
 '您的包裹已发货 — {{orderNo}}',
 '<p>您好，{{userName}}，</p><p>订单 <strong>{{orderNo}}</strong> 已发出，物流单号：<strong>{{trackingNo}}</strong>（{{carrier}}）。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('ORDER_SHIPPED', 'EMAIL', 'en',
 'Your order {{orderNo}} has shipped',
 '<p>Hi {{userName}},</p><p>Order <strong>{{orderNo}}</strong> is on its way! Tracking number: <strong>{{trackingNo}}</strong> ({{carrier}}).</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- ORDER_DELIVERED ─────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('ORDER_DELIVERED', 'EMAIL', 'zh-CN',
 '您的包裹已送达 — {{orderNo}}',
 '<p>您好，{{userName}}，</p><p>订单 <strong>{{orderNo}}</strong> 已签收，希望您满意！欢迎留下评价。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('ORDER_DELIVERED', 'EMAIL', 'en',
 'Order {{orderNo}} delivered',
 '<p>Hi {{userName}},</p><p>Your order <strong>{{orderNo}}</strong> has been delivered. We hope you love it — leave a review!</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- RMA_APPROVED ────────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('RMA_APPROVED', 'EMAIL', 'zh-CN',
 '您的售后申请 {{rmaNo}} 已批准',
 '<p>您好，{{userName}}，</p><p>您的{{rmaType}}申请 <strong>{{rmaNo}}</strong> 已审核通过。请按照指引寄回商品。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('RMA_APPROVED', 'EMAIL', 'en',
 'RMA {{rmaNo}} approved',
 '<p>Hi {{userName}},</p><p>Your {{rmaType}} request <strong>{{rmaNo}}</strong> has been approved. Please follow the return instructions to ship the item back.</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- RMA_REJECTED ────────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('RMA_REJECTED', 'EMAIL', 'zh-CN',
 '您的售后申请 {{rmaNo}} 未通过',
 '<p>您好，{{userName}}，</p><p>很遗憾，您的{{rmaType}}申请 <strong>{{rmaNo}}</strong> 未能通过审核。原因：{{reason}}。如有疑问，请联系客服。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('RMA_REJECTED', 'EMAIL', 'en',
 'RMA {{rmaNo}} rejected',
 '<p>Hi {{userName}},</p><p>Unfortunately your {{rmaType}} request <strong>{{rmaNo}}</strong> was not approved. Reason: {{reason}}. Please contact support if you have questions.</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- RMA_RECEIVED ────────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('RMA_RECEIVED', 'EMAIL', 'zh-CN',
 '我们已收到您的退货 — {{rmaNo}}',
 '<p>您好，{{userName}}，</p><p>我们已收到您寄回的商品（售后单号：<strong>{{rmaNo}}</strong>），正在进行质检，结果将在 1-3 个工作日内通知您。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('RMA_RECEIVED', 'EMAIL', 'en',
 'Return received — {{rmaNo}}',
 '<p>Hi {{userName}},</p><p>We have received your returned item for RMA <strong>{{rmaNo}}</strong>. Quality inspection will be completed within 1-3 business days.</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- RMA_COMPLETED ───────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('RMA_COMPLETED', 'EMAIL', 'zh-CN',
 '售后处理完成 — {{rmaNo}}',
 '<p>您好，{{userName}}，</p><p>您的售后申请 <strong>{{rmaNo}}</strong> 已处理完成。退款/换货将按约定方式执行，感谢您的耐心等待。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('RMA_COMPLETED', 'EMAIL', 'en',
 'RMA {{rmaNo}} completed',
 '<p>Hi {{userName}},</p><p>Your RMA <strong>{{rmaNo}}</strong> has been completed. Refund or replacement will be processed as agreed. Thank you for your patience.</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- REVIEW_APPROVED ─────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('REVIEW_APPROVED', 'EMAIL', 'zh-CN',
 '您的评价已发布',
 '<p>您好，{{userName}}，</p><p>您对「{{productName}}」的评价已通过审核并发布，感谢您的分享！</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('REVIEW_APPROVED', 'EMAIL', 'en',
 'Your review has been published',
 '<p>Hi {{userName}},</p><p>Your review for "{{productName}}" has been approved and published. Thank you for sharing!</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- REVIEW_REJECTED ─────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('REVIEW_REJECTED', 'EMAIL', 'zh-CN',
 '您的评价未能发布',
 '<p>您好，{{userName}}，</p><p>很遗憾，您对「{{productName}}」的评价未能通过审核。原因：{{reason}}。</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('REVIEW_REJECTED', 'EMAIL', 'en',
 'Your review could not be published',
 '<p>Hi {{userName}},</p><p>Unfortunately your review for "{{productName}}" was not approved. Reason: {{reason}}.</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- COUPON_EXPIRING ─────────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('COUPON_EXPIRING', 'EMAIL', 'zh-CN',
 '您的优惠券即将到期',
 '<p>您好，{{userName}}，</p><p>您的优惠券 <strong>{{couponCode}}</strong> 将于 {{expiresAt}} 到期，记得在到期前使用哦！</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('COUPON_EXPIRING', 'EMAIL', 'en',
 'Your coupon is expiring soon',
 '<p>Hi {{userName}},</p><p>Your coupon <strong>{{couponCode}}</strong> expires on {{expiresAt}}. Use it before it''s gone!</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

-- WISHLIST_PRICE_DROP ─────────────────────────────────────────────────────────
INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('WISHLIST_PRICE_DROP', 'EMAIL', 'zh-CN',
 '好消息！您收藏的商品降价了',
 '<p>您好，{{userName}}，</p><p>您收藏的「<strong>{{productName}}</strong>」现在只需 {{currency}} <strong>{{newPrice}}</strong>，心动不如行动！</p>')
ON CONFLICT (type, channel, language) DO NOTHING;

INSERT INTO notification_templates (type, channel, language, subject, body) VALUES
('WISHLIST_PRICE_DROP', 'EMAIL', 'en',
 'Price drop on your wishlist item!',
 '<p>Hi {{userName}},</p><p>Great news! "<strong>{{productName}}</strong>" is now only {{currency}} <strong>{{newPrice}}</strong>. Don''t miss out!</p>')
ON CONFLICT (type, channel, language) DO NOTHING;
