# iemodo 电商系统稳定性防护 Todo List

## 📋 说明
基于 P0 级灾难风险梳理的防护清单，按优先级排序。

---

## 🔴 高优先级（必须做）

### 1. 库存防超卖机制

#### 1.1 现状检查 ✅
| 检查项 | 状态 | 说明 |
|--------|------|------|
| Redis 原子扣减 | ✅ 已实现 | `InventoryRedisService` 使用 Lua 脚本 |
| 数据库乐观锁 | ✅ 已实现 | `@Version` + 原子 UPDATE |
| 预留模式 | ✅ 已实现 | `available - reserved = sellable` |
| 订单取消回滚 | ✅ 已实现 | `cancelOrder` 调用 Redis rollback |

#### 1.2 待完成任务
- [x] **Order 实体加乐观锁**
  ```java
  @Version
  private Integer version;
  ```
- [x] **支付超时自动关单**（延迟任务表方案）
  - 创建 `order_delay_task` 表（字段：order_id, task_type, execute_time, status）
  - 定时任务每 30 秒扫描执行
  - 关单操作使用乐观锁防止并发
  - 关单后回滚 Redis 库存
- [x] **库存扣减幂等性（后端令牌模式）**
  ```
  流程：后端生成令牌 → 前端携带令牌 → 后端校验令牌

  1. 下单时生成幂等 key：deduct:{tenantId}:{orderNo}
  2. Redis Lua 脚本校验幂等状态：
     - PENDING → 执行扣减 → 标记 SUCCESS
     - SUCCESS → 直接返回（已处理过）
     - 不存在 → 返回错误
  ```
- [x] **Redis 与数据库同步机制**
  - `syncCacheFor()` 在 reserve/release/inbound/adjust 后将 DB 真实值写回 Redis

### 2. 支付状态一致性
- [x] 建立支付回调幂等表（唯一索引）— `payment_callback_log` + `UNIQUE(event_id)`
- [x] 实现支付状态机（PENDING → PROCESSING → SUCCESS/FAILED/CANCELLED 等）
- [x] 设计定时对账任务（`PaymentReconciliationScheduler`，每 5 分钟查 Stripe 补偿）
- [x] 实现支付异常补偿机制（重复回调去重 + 丢单主动对账）

### 3. 促销价格计算防护
- [x] 梳理现有优惠计算逻辑 — 发现并修复 PERCENTAGE 100x Bug
- [x] 设计价格计算灰度验证方案 — `PricingGuard` 接入 `applySegmentAndBuild()`
- [x] 实现促销配置预发布机制 — 默认草稿 + `CouponPublishScheduler` 定时发布
- [x] 添加价格计算异常报警 — `PricingGuard` 打印 `[PRICE_ANOMALY]` 结构化日志

---

## 🟡 中优先级（尽快做）

### 4. 限流与熔断
- [x] 接入 Sentinel（Spring Cloud Alibaba 生态）— api-gateway 已集成
- [x] 配置核心接口限流规则（下单/支付/库存查询）— 补充了 payment/inventory/pricing 路由
- [x] 设计降级策略 — `CustomBlockExceptionHandler` 返回结构化 JSON（429/503）
- [x] 实现熔断自动恢复机制 — `DegradeRule` OPEN→HALF_OPEN→CLOSED，注册状态变化日志

### 5. 缓存防护
- [ ] 添加 Redis 缓存预热机制（大促前）
- [ ] 实现热点 Key 自动发现与本地缓存兜底
- [ ] 配置缓存穿透防护（布隆过滤器/空值缓存）
- [ ] 设计缓存雪崩降级方案

### 6. 系统容量保障
- [ ] 建立核心链路压测基准（下单/支付）
- [ ] 配置 JVM 内存监控与 OOM 预警
- [ ] 优化 DB 连接池配置（R2DBC 连接池大小）
- [x] 设计数据库慢查询自动熔断 — `SlowQueryCircuitBreaker` + r2dbc-proxy，三层 ConnectionFactory 包装

---

## 🟢 低优先级（持续优化）

### 7. 可观测性增强
- [ ] 完善核心链路监控（库存/支付/优惠）
- [ ] 配置关键业务指标报警（超卖率/支付成功率）
- [ ] 实现分布式链路追踪（Sleuth/Micrometer）
- [ ] 建立大促期间值班检查清单

### 8. 灾备与演练
- [ ] 制定故障演练计划（库存超卖模拟）
- [ ] 设计数据备份与恢复方案
- [ ] 建立故障应急手册（Runbook）
- [ ] 定期进行压测与演练

---

## 📅 建议时间线

| 阶段 | 时间 | 目标 |
|------|------|------|
| Phase 1 | 1-2 周 | 完成高优先级项（库存/支付/促销） |
| Phase 2 | 3-4 周 | 完成限流/熔断/缓存防护落地 |
| Phase 3 | 持续 | 完善监控、演练、文档 |

---

## 📝 备注

- 技术栈参考：Java 21, Spring Boot 3.3, Spring Cloud Alibaba, WebFlux/R2DBC, Redis, PostgreSQL
- 建议先从 **库存防超卖** 和 **支付幂等** 开始，这两项直接关联资金安全

### 关键决策记录
- **乐观锁方案**：使用 `@Version` 注解，直接加在 Order 实体上
- **幂等性方案**：后端生成令牌 → 前端携带令牌 → 后端校验令牌（Lua 脚本内完成）
- **超时关单**：延迟任务表 + 定时扫描（30秒），任务状态机保证单实例执行
- **限流熔断**：Sentinel Gateway（流控）+ DegradeRule（熔断）+ CustomBlockExceptionHandler（降级 JSON）
- **慢查询熔断**：r2dbc-proxy 计时 → SlowQueryCircuitBreaker 滑动窗口 → CircuitBreakerConnectionFactory 拒绝连接

---

*创建于 2025-04-04*
*更新于 2026-04-14 - 完成 Section 1-4 全部高/中优先级任务，Section 6 慢查询熔断*
