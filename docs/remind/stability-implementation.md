# iemodo 稳定性防护实施记录

> 对应 `docs/plan/stability-todo.md` 高优先级任务
> 实施时间：2026-04-12

---

## Section 1 — 库存防超卖机制

### 1.1 Order 实体乐观锁

**文件：** `order-service/.../domain/Order.java`

在 Order 实体上加了 `@Version private Integer version`，配合 Spring Data R2DBC 的乐观锁机制。并发更新时若版本不匹配抛 `OptimisticLockingFailureException`，上层捕获后重试或返回冲突错误。

**迁移：** `order-service/.../db/migration/V2__add_order_version.sql`
```sql
ALTER TABLE orders ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
```

---

### 1.2 支付超时自动关单（延迟任务表方案）

**新建文件：**
- `order-service/.../domain/DelayTaskStatus.java` — 枚举：PENDING / PROCESSING / DONE / SKIPPED
- `order-service/.../domain/OrderDelayTask.java` — 延迟任务实体
- `order-service/.../repository/OrderDelayTaskRepository.java` — 含原子 `claimTask()` 防并发重复执行
- `order-service/.../service/OrderTimeoutScheduler.java` — `@Scheduled(fixedDelay=30s)`，扫描到期任务执行关单

**迁移：** `V3__create_order_delay_task.sql` — 创建 `order_delay_task` 表

**关单流程：**
1. 下单时写入 `order_delay_task`（execute_time = now + 30min）
2. Scheduler 每 30s 扫描 PENDING 且 execute_time 已到期的任务
3. `claimTask()` 原子 UPDATE PENDING→PROCESSING（防多实例重复执行）
4. 关单 + 回滚 Redis 库存
5. 任务置为 DONE

---

### 1.3 库存扣减幂等性（后端令牌模式）

**修改文件：**
- `order-service/.../service/InventoryRedisService.java` — 新增 Lua 脚本：`registerToken`、`checkIdempotency`、`markSuccess`
- `order-service/.../service/OrderService.java` — 下单时校验令牌，扣减成功后标记 SUCCESS
- `order-service/.../dto/CreateOrderRequest.java` — 新增 `idempotencyKey` 字段
- `order-service/.../dto/OrderTokenResponse.java` — 新建
- `order-service/.../controller/OrderController.java` — 新增 `POST /orders/token`

**流程：**
1. 前端调 `POST /orders/token` 获取 `idempotencyKey`（Redis 写 PENDING，TTL 10min）
2. 下单请求携带 `idempotencyKey`
3. Lua 脚本原子校验：PENDING → 执行；SUCCESS → 幂等返回；NOT_FOUND → 拒绝
4. 扣减成功后 `markSuccess`（TTL 24h）

**ErrorCode 新增：**
- `DUPLICATE_ORDER(4032)` — 重复下单
- `INVALID_IDEMPOTENCY_TOKEN(4033)` — 令牌不存在或已过期

---

### 1.4 Redis 与数据库库存同步

**修改文件：** `inventory-service/.../service/InventoryService.java`

新增 `syncCacheFor(tenantId, warehouseId, skuId)` 私有方法：重新从 DB 查最新 sellable 数量，写绝对值到 Redis（避免 delta 漂移）。

在以下操作后调用同步：`reserveStock`、`releaseStock`、`inbound`、`adjust`

同时修改 `InventoryController`，为上述接口补加 `X-TenantID` header 传参。

---

## Section 2 — 支付状态一致性

### 2.1 Stripe 回调幂等表

**新建文件：**
- `payment-service/.../domain/PaymentCallbackLog.java`
- `payment-service/.../repository/PaymentCallbackLogRepository.java`
- `payment-service/.../db/migration/V2__payment_callback_idempotency.sql`

```sql
CREATE TABLE payment_callback_log (
    event_id VARCHAR(128) NOT NULL,
    CONSTRAINT uq_callback_event_id UNIQUE (event_id)
);
```

**`PaymentService.handleStripeWebhook`** 改造：先写 `PaymentCallbackLog`，捕获 `DataIntegrityViolationException`（唯一键冲突）静默丢弃重复事件。

---

### 2.2 支付状态机

**修改文件：** `payment-service/.../domain/Payment.java`

在 `PaymentStatus` 枚举上增加 `allowedTransitions()` 方法定义合法转移路径，并加 `canTransitionTo()` / `transitionTo()` 方法，非法转移抛 `INVALID_PAYMENT_STATUS(4034)` 异常。

合法路径：
```
PENDING     → PROCESSING, SUCCESS, FAILED, CANCELLED
PROCESSING  → SUCCESS, FAILED, CANCELLED
SUCCESS     → REFUNDED, PARTIALLY_REFUNDED
PARTIALLY_REFUNDED → REFUNDED, PARTIALLY_REFUNDED
FAILED / CANCELLED / REFUNDED → 终态，不可转移
```

---

### 2.3 定时对账任务

**新建文件：** `payment-service/.../service/PaymentReconciliationScheduler.java`

两个调度任务：
- `cancelExpiredPayments()` — `@Scheduled(fixedDelay=60s)`，取消已过期 PENDING 支付
- `reconcileStuckPayments()` — `@Scheduled(fixedDelay=5min)`，查询 PENDING/PROCESSING 超 10 分钟且有 Stripe txnId 的支付，调 Stripe API 比对状态并同步

**修改：** `PaymentRepository` 新增 `findStuckPayments(Instant cutoff)`

---

### 2.4 支付异常补偿

- 重复 webhook：通过 `payment_callback_log` 唯一索引 + `DataIntegrityViolationException` 去重
- 丢单/丢 webhook：`reconcileStuckPayments` 主动查 Stripe 补偿
- `reconcilePayment()` 处理三种 Stripe 状态：`succeeded` / `canceled` / `requires_payment_method`

**修改：** `PaymentServiceApplication.java` 增加 `@EnableScheduling`

---

## Section 3 — 促销价格计算防护

### 3.1 修复 PERCENTAGE 优惠券 100x Bug

**文件：** `marketing-service/.../domain/Coupon.java`

`calculateDiscount()` 原代码直接 `orderAmount.multiply(discountValue)`（discountValue=20 时乘以 20 而非 0.20），修复为：
```java
discountValue.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
```
并加 `.setScale(2, RoundingMode.HALF_UP)` 保证精度。

---

### 3.2 促销配置预发布机制

**修改文件：**
- `CreateCouponRequest.java` — `isActive` 字段默认 `null`，创建时默认为 `false`（草稿状态）
- `CouponServiceImpl.createCoupon` — `isActive` 改为 `Boolean.TRUE.equals(request.getIsActive())`
- `CouponService` — 新增 `publishCoupon` / `unpublishCoupon` 接口
- `CouponServiceImpl` — 实现上述两个方法
- `CouponRepository` — 新增 `findScheduledToActivate(Instant now)`
- `CouponController` — 新增 `POST /coupons/{id}/publish` 和 `POST /coupons/{id}/unpublish`
- `MarketingServiceApplication` — 增加 `@EnableScheduling`

**新建：** `CouponPublishScheduler.java` — `@Scheduled(fixedDelay=60s)`，自动发布 `valid_from` 已到期的草稿优惠券，打印日志

---

### 3.3 价格计算异常报警（PricingGuard）

**新建：** `pricing-service/.../service/PricingGuard.java`

检测以下异常并打印 `[PRICE_ANOMALY]` 结构化日志：
- `finalPrice ≤ 0`
- `discounts < 0`（价格被抬高）
- `discounts > basePrice`（折扣超过原价）
- `discounts / basePrice > 90%`

**修改：** `PricingServiceImpl.applySegmentAndBuild()` — 在返回前调 `pricingGuard.validate(detail, sku)`

---

## Section 4 — 限流与熔断（API Gateway）

### 4.1 补充缺失路由限流

**修改：** `api-gateway/.../resources/application.yml`

| 路由 | 变更 |
|------|------|
| `payment-service-route` | 补加 `RequestRateLimiter`（100 replenish / 200 burst） |
| `inventory-service-route` | **新增路由**（`/inv/**`），限流 300/600 |
| `pricing-service-route` | 补加 `RequestRateLimiter`（200/400） |

---

### 4.2 Sentinel 熔断规则（DegradeRule）

**重写：** `api-gateway/.../config/SentinelConfig.java`

修复原有重复 `user-service-route` 规则 bug；新增 Sentinel 流控规则覆盖 payment / inventory / marketing / pricing 路由。

新增 `DegradeRule` 熔断规则：

| 路由 | 策略 | 阈值 | 最小请求数 | 恢复窗口 |
|------|------|------|-----------|---------|
| order | 错误率 | > 50% | 10 | 20s |
| payment | 错误率 | > 30% | 5 | 30s |
| payment | 慢调用比例 | > 80%（>2000ms） | 5 | 30s |
| inventory | 错误率 | > 50% | 10 | 15s |

注册 `CircuitBreakerStateChangeObserver`，所有状态变化打印 `[CIRCUIT_BREAKER]` 日志。

---

### 4.3 结构化降级响应

**新建：** `api-gateway/.../config/CustomBlockExceptionHandler.java`

替换默认 `SentinelGatewayBlockExceptionHandler`，返回统一 JSON：
- `FlowException` → HTTP 429 `{"errorCode":"TOO_MANY_REQUESTS"}`
- `DegradeException` → HTTP 503 `{"errorCode":"SERVICE_DEGRADED"}` + `[CIRCUIT_OPEN]` 日志

---

## Section 6（部分）— 慢查询熔断

**新建文件（common 模块）：**

| 文件 | 职责 |
|------|------|
| `db/SlowQueryProperties` | 配置项（thresholdMs / circuitOpenThreshold / windowSeconds / recoverySeconds） |
| `db/SlowQueryCircuitBreaker` | 滑动窗口状态机 CLOSED→OPEN→HALF_OPEN→CLOSED |
| `db/SlowQueryProxyListener` | r2dbc-proxy `afterQuery` 计时，慢查询通知熔断器 |
| `db/CircuitBreakerConnectionFactory` | `create()` 先检查熔断状态，OPEN 时返回 `Mono.error` |
| `db/SlowQueryCircuitOpenException` | 熔断打开异常 |

**修改文件：**
- `common/pom.xml` — 添加 `r2dbc-proxy 1.1.5.RELEASE`
- `MultitenantR2dbcConfiguration` — `createFactory()` 新增三层包装（PostgreSQL → Proxy → CircuitBreaker）
- `GlobalExceptionHandler` — `SlowQueryCircuitOpenException` 映射到 HTTP 503

**三层包装架构：**
```
CircuitBreakerConnectionFactory  ← 连接级熔断门（OPEN 时直接拒绝）
  └── ProxyConnectionFactory     ← r2dbc-proxy 计时拦截
        └── PostgresqlConnectionFactory
```

**默认参数（可通过 application.yml 覆盖）：**
```yaml
iemodo:
  db:
    slow-query:
      threshold-ms: 500           # 超过此值视为慢查询
      circuit-open-threshold: 10  # 窗口内慢查询次数触发熔断
      window-seconds: 60          # 计数窗口
      recovery-seconds: 30        # 熔断打开后恢复探测等待时间
      circuit-breaker-enabled: true
```
