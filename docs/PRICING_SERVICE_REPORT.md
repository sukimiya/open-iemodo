# Pricing Service 实施报告

**日期**: 2026-03-28  
**服务**: Pricing Service (端口 8086)  
**状态**: ✅ 已完成

---

## 📋 功能总览

| 功能模块 | 状态 | 说明 |
|---------|------|------|
| 汇率服务 | ✅ | 多级缓存 (Caffeine + Redis + DB/API) |
| 货币转换 | ✅ | 支持 20+ 种货币 |
| 国际定价 | ✅ | 区域定价、Markup、折扣 |
| 多级缓存 | ✅ | L1 (10min) + L2 (1hour) + L3 (API) |
| 汇率历史 | ✅ | 历史汇率查询和存档 |

---

## 🗄️ 数据库设计

### Schema: `pricing`

**currencies 表** - 货币信息
```sql
- code (ISO 4217): USD, EUR, CNY, etc.
- name, symbol, decimal_places
- is_active, is_base_currency
```

**exchange_rates 表** - 汇率历史
```sql
- from_currency, to_currency
- rate, inverse_rate
- source, api_provider
- recorded_at
```

**regional_pricing_config 表** - 区域定价配置
```sql
- country_code, sku
- markup_multiplier (价格倍率)
- min_price, max_price
- pricing_strategy (STANDARD/DYNAMIC/COMPETITIVE)
- effective_from, effective_to
```

**quantity_discount_tiers 表** - 数量折扣
```sql
- sku, country_code
- min_quantity, max_quantity
- discount_percent
```

**segment_pricing 表** - 客户分组定价
```sql
- segment_code (VIP, WHOLESALE, STUDENT)
- sku, discount_percent
```

---

## 🔧 核心实现

### ExchangeRateService 多级缓存架构

```
┌─────────────────────────────────────────────┐
│  L1: Caffeine (Local Cache)                  │
│  - TTL: 10 minutes                           │
│  - Max Size: 1000 entries                    │
│  - In-memory, fastest access                 │
└───────────────────┬─────────────────────────┘
                    │ Miss
                    ▼
┌─────────────────────────────────────────────┐
│  L2: Redis (Distributed Cache)               │
│  - TTL: 1 hour                               │
│  - Shared across instances                   │
│  - Survives app restart                      │
└───────────────────┬─────────────────────────┘
                    │ Miss
                    ▼
┌─────────────────────────────────────────────┐
│  L3: Database / External API                 │
│  - PostgreSQL: Historical rates              │
│  - Fixer.io / ExchangeRate-API               │
│  - Fallback to cross-rate calculation        │
└─────────────────────────────────────────────┘
```

### 汇率转换流程
1. **Check L1**: Caffeine local cache (10 min TTL)
2. **Check L2**: Redis distributed cache (1 hour TTL)
3. **Check L3**: PostgreSQL (recent rates within 1 hour)
4. **Cross Rate**: Via USD (EUR->USD->CNY)
5. **External API**: Fixer.io (fallback with mock rates)

### 定价计算流程
```
Base Price (USD)
    ↓
Regional Markup ×1.15 (e.g., China)
    ↓
Currency Conversion → CNY
    ↓
Quantity Discount (qty > 10: -5%)
    ↓
Segment Discount (VIP: -10%)
    ↓
Final Price
```

---

## 📡 API 接口

### 汇率接口
```
GET    /pricing/api/v1/exchange-rates?from=USD&to=EUR
GET    /pricing/api/v1/exchange-rates/batch?from=USD&to=EUR,GBP,CNY
POST   /pricing/api/v1/exchange-rates/convert
       Body: { "amount": 100, "fromCurrency": "USD", "toCurrency": "EUR" }
GET    /pricing/api/v1/exchange-rates/currencies
GET    /pricing/api/v1/exchange-rates/history?from=USD&to=EUR&fromDate=...&toDate=...
POST   /pricing/api/v1/exchange-rates/refresh
```

### 定价接口
```
POST   /pricing/api/v1/price/calculate
       Params: sku, countryCode, quantity, customerSegment
POST   /pricing/api/v1/cart/calculate
       Body: CartPricingRequest { countryCode, items[], customerSegment }
GET    /pricing/api/v1/price/convert?price=100&from=USD&to=EUR
```

---

## 🧪 测试覆盖

| 测试类 | 测试方法 | 说明 |
|--------|----------|------|
| ExchangeRateServiceTest | 7 个 | 汇率查询、转换、缓存 |
| PricingServiceTest | 2 个 | 价格转换、折扣计算 |

### 测试场景
- ✅ 同货币转换返回 1
- ✅ L1/L2 缓存命中
- ✅ 批量汇率查询
- ✅ 金额转换计算
- ✅ 货币支持检查
- ✅ 价格转换
- ✅ 分组折扣

---

## 📝 配置示例

```yaml
# application.yml
server:
  port: 8086

spring:
  config:
    import: "nacos:pricing-service.yaml?group=IEMODO"

exchange:
  fixer:
    api-key: ${FIXER_API_KEY:your-api-key}
  base-currency: USD
  cache:
    l1-ttl-minutes: 10
    l2-ttl-hours: 1
```

---

## 🚀 启动方式

```bash
# 环境变量配置
export FIXER_API_KEY=your_api_key  # Optional, has fallback rates

# 启动服务
JAVA_HOME=/opt/homebrew/opt/openjdk@21 \
/opt/homebrew/bin/mvn spring-boot:run -pl pricing-service
```

---

## 📊 项目统计

| 指标 | 数值 |
|------|------|
| 新增文件 | 22 个 |
| Java 代码 | ~2,500 行 |
| 测试类 | 2 个 |
| 测试方法 | 9 个 |
| 数据库表 | 5 张 |
| API 端点 | 8 个 |
| 支持货币 | 20+ |

---

## 🔗 依赖关系

```
pricing-service → common (multitenancy, security)
               → PostgreSQL (pricing schema)
               → Redis (L2 cache)
               → Caffeine (L1 cache)
               → Fixer.io / ExchangeRate-API (L3)
```

---

## 💡 关键特性

### 1. 多级缓存策略
- **L1 (Caffeine)**: 超高速本地访问，10分钟过期
- **L2 (Redis)**: 分布式共享，1小时过期
- **L3 (DB/API)**: 持久化存储，外部API获取

### 2. 交叉汇率计算
当直接汇率不存在时，自动通过 USD 计算：
```
EUR → CNY = (EUR → USD) × (USD → CNY)
```

### 3. 弹性降级
- 外部 API 失败时使用 mock 汇率
- 所有计算都有默认值保证服务可用

### 4. 区域定价策略
- **STANDARD**: 标准定价
- **DYNAMIC**: 动态定价
- **COMPETITIVE**: 竞争定价

---

**报告完成时间**: 2026-03-28 19:15:00+08:00  
**项目状态**: 🟢 Pricing Service 完成，测试全部通过
