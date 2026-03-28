# Payment Service 实施报告

**日期**: 2026-03-28  
**服务**: Payment Service (端口 8085)  
**状态**: ✅ 已完成

---

## 📋 功能总览

| 功能模块 | 状态 | 说明 |
|---------|------|------|
| 支付单管理 | ✅ | 创建、查询、支付状态跟踪 |
| Stripe 集成 | ✅ | PaymentIntent 创建、确认、查询 |
| 退款处理 | ✅ | 全额/部分退款、退款审核 |
| Webhook 处理 | ✅ | Stripe 回调处理（支付成功/失败/退款）|
| 多租户支持 | ✅ | Schema-per-Tenant 隔离 |
| PCI DSS 合规 | ✅ | 不存储完整卡号、敏感数据加密 |

---

## 🗄️ 数据库设计

### Schema: `payment_{tenantId}`

**payments 表** - 支付单主表
```sql
- id, payment_no (唯一), order_id, order_no, customer_id
- amount, currency
- channel (STRIPE/PAYPAL/ALIPAY/WECHAT_PAY)
- channel_sub_type (CARD/IDEAL/BANCONTACT...)
- status (PENDING/PROCESSING/SUCCESS/FAILED/CANCELLED/REFUNDED)
- third_party_txn_id, third_party_txn_data (JSONB)
- paid_at, expired_at (默认30分钟)
- refunded_amount, refundable_amount (计算列)
- failure_code, failure_message
```

**refunds 表** - 退款单表
```sql
- id, refund_no (唯一), payment_id, order_id
- amount, currency
- status (PENDING/PROCESSING/SUCCESS/FAILED)
- reason_type (CUSTOMER_REQUEST/DUPLICATE/FRAUDULENT...)
- third_party_refund_id
- processed_at
```

**payment_audit_logs 表** - 审计日志（合规）

---

## 🔧 核心实现

### PaymentProvider 接口
统一的支付渠道抽象，支持多提供商：
- `createPaymentIntent()` - 创建支付意图
- `confirmPayment()` - 确认支付
- `retrievePayment()` - 查询支付状态
- `refund()` - 退款
- `processWebhook()` - 处理 Webhook

### StripePaymentProvider
Stripe SDK 的响应式包装：
```java
// 使用 Mono.fromCallable() 包装同步调用
return Mono.fromCallable(() -> {
    PaymentIntent intent = PaymentIntent.create(params);
    return mapToResult(intent);
}).subscribeOn(Schedulers.boundedElastic())
  .onErrorResume(StripeException.class, e -> ...);
```

### PaymentService 核心方法
| 方法 | 功能 |
|------|------|
| `createPaymentIntent()` | 创建支付单 + Stripe PaymentIntent |
| `confirmPayment()` | 确认支付（前端回调后调用）|
| `cancelPayment()` | 取消待支付订单 |
| `createRefund()` | 创建退款 + Stripe Refund |
| `handleStripeWebhook()` | 处理 Stripe Webhook 事件 |
| `processExpiredPayments()` | 定时任务：清理过期支付 |

---

## 📡 API 接口

### 支付接口
```
POST   /pay/api/v1/payments/intents              # 创建支付意图
GET    /pay/api/v1/payments/{paymentId}          # 查询支付
GET    /pay/api/v1/payments/by-no/{paymentNo}    # 按编号查询
GET    /pay/api/v1/orders/{orderId}/payments     # 查询订单支付
POST   /pay/api/v1/payments/{paymentId}/confirm  # 确认支付
POST   /pay/api/v1/payments/{paymentId}/cancel   # 取消支付
POST   /pay/api/v1/payments/{paymentId}/refund   # 申请退款
GET    /pay/api/v1/refunds/{refundId}            # 查询退款
GET    /pay/api/v1/payments/{paymentId}/refunds  # 查询支付的所有退款
```

### Webhook 接口
```
POST   /pay/api/v1/webhooks/stripe               # Stripe Webhook 回调
```

---

## 🛡️ 安全与合规

### PCI DSS 合规措施
1. **不存储完整卡号** - 只保留后4位
2. **不存储 CVV** - 完全不在系统中出现
3. **Tokenization** - 使用 Stripe PaymentMethod ID
4. **HTTPS 传输** - 所有 API 强制 HTTPS
5. **审计日志** - 所有支付操作记录审计日志

### 状态机
```
PENDING → PROCESSING → SUCCESS → REFUNDED
    ↓         ↓           ↓
CANCELLED  FAILED    PARTIALLY_REFUNDED
```

---

## 🧪 测试覆盖

| 测试类 | 测试方法 | 说明 |
|--------|----------|------|
| PaymentServiceTest | 9 个 | 单元测试覆盖核心逻辑 |

### 测试场景
- ✅ 创建支付意图成功
- ✅ 查询支付
- ✅ 取消支付
- ✅ 创建退款成功
- ✅ 退款金额超限检查
- ✅ 不可退款状态检查
- ✅ 支付过期处理

---

## 📝 配置示例

```yaml
# application.yml
stripe:
  public-key: ${STRIPE_PUBLIC_KEY:pk_test_xxx}
  secret-key: ${STRIPE_SECRET_KEY:sk_test_xxx}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET:whsec_xxx}

spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/iemodo_platform
  flyway:
    schemas: payment_tenant_001, payment_tenant_002
```

---

## 🚀 启动方式

```bash
# 环境变量配置
export STRIPE_PUBLIC_KEY=pk_test_xxx
export STRIPE_SECRET_KEY=sk_test_xxx
export STRIPE_WEBHOOK_SECRET=whsec_xxx

# 启动服务
JAVA_HOME=/opt/homebrew/opt/openjdk@21 \
/opt/homebrew/bin/mvn spring-boot:run -pl payment-service
```

---

## 📊 项目统计

| 指标 | 数值 |
|------|------|
| 新增文件 | 16 个 |
| Java 代码 | ~1,800 行 |
| 测试类 | 1 个 |
| 测试方法 | 9 个 |
| 数据库表 | 3 张 |
| API 端点 | 10 个 |

---

## 🔗 依赖关系

```
payment-service → common (multitenancy, security, response)
               → PostgreSQL (payment_{tenantId} schema)
               → Redis (optional: rate limiting)
               → Stripe SDK (v24.0.0)
```

---

**报告完成时间**: 2026-03-28 16:30:00+08:00  
**项目状态**: 🟢 Payment Service 完成，9个测试全部通过
