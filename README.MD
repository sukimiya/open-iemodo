<br />

# iemodo - 跨境电商微服务系统

![Java](https://img.shields.io/badge/Java-21-green?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen?logo=spring)
![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0-blue)
![WebFlux](https://img.shields.io/badge/WebFlux-Reactive-brightblue)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

**一个面向全球、支持多租户的开源跨境电商微服务系统，采用响应式架构、云原生设计。**

***

## 📋 项目概述

**iemodo** 是一个基于 Spring Cloud Alibaba 和 Spring WebFlux 的**企业级跨境电商微服务平台**，专为全球商家提供多店铺管理、订单处理、支付集成等核心功能。该项目采用**全链路响应式架构**和**Schema-per-Tenant 多租户隔离方案**，支持高并发场景（单节点 1000+ QPS）。

### 核心特性

✅ **全链路响应式**：基于 Project Reactor（Mono/Flux），完全非阻塞 I/O  
✅ **多租户 SaaS**：Schema-per-Tenant 隔离 + 独立数据库可选  
✅ **微服务架构**：Spring Cloud Alibaba（Nacos + Gateway + Sentinel）  
✅ **海外部署**：AWS RDS + ElastiCache + S3，满足 GDPR/CCPA 合规  
✅ **生产就绪**：完整的认证体系、审计日志、监控告警

***

## 🏗️ 技术架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CDN + WAF                                    │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────┐
│              API Gateway (Spring Cloud Gateway)                     │
│              ├─ Nacos 服务发现与负载均衡                              │
│              ├─ Sentinel 限流熔断（租户级）                           │
│              ├─ JWT 预校验 GlobalFilter                              │
│              └─ TraceID 链路追踪                                     │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼───────┐    ┌───────▼───────┐    ┌───────▼───────┐
│  User         │    │  Order        │    │  Product      │
│  Service      │    │  Service      │    │  Service      │
│  :8081        │    │  :8083        │    │  :8082        │
│               │    │               │    │               │
│  ┌──────────┐ │    │  ┌──────────┐ │    │  ┌──────────┐ │
│  │ WebFlux  │ │    │  │ WebFlux  │ │    │  │ WebFlux  │ │
│  ├──────────┤ │    │  ├──────────┤ │    │  ├──────────┤ │
│  │ R2DBC    │ │    │  │ R2DBC    │ │    │  │ R2DBC    │ │
│  │ DAO      │ │    │  │ DAO      │ │    │  │ DAO      │ │
│  └──────────┘ │    │  └──────────┘ │    │  └──────────┘ │
└───────┬───────┘    └───────┬───────┘    └───────┬───────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────┐
│                        共享基础设施                                  │
│                                                                     │
│    ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐ │
│    │ PostgreSQL       │  │ Redis Cluster    │  │ RocketMQ         │ │
│    │ Cluster          │  │ + Lua            │  │                  │ │
│    │ (Schema-per-     │  │ (Caffeine L1     │  │ (事务消息/        │ │
│    │  Tenant)         │  │  Cache)          │  │  异步任务)        │ │
│    └──────────────────┘  └──────────────────┘  └──────────────────┘ │
│    ┌──────────────────┐                                             │
│    │ Nacos Config     │                                             │
│    │ Center           │                                             │
│    │ (服务配置/       │                                             │
│    │  租户管理)        │                                             │
│    └──────────────────┘                                             │
└─────────────────────────────────────────────────────────────────────┘
```

### 核心技术栈

| 分类          | 技术选型                     | 版本        | 说明                         |
| ----------- | ------------------------ | --------- | -------------------------- |
| **基础框架**    | Spring Boot              | 3.3.0+    | 最新 LTS 版本                  |
| **响应式编程**   | Spring WebFlux           | 6.2.0+    | 全链路异步非阻塞                   |
| **响应式数据访问** | Spring Data R2DBC        | 3.3.0     | 非阻塞数据库驱动                   |
| **编程模型**    | Project Reactor          | 3.6.0     | Mono<T> / Flux<T>          |
| **微服务框架**   | Spring Cloud Alibaba     | 2023.0.0+ | Nacos + Gateway + Sentinel |
| **多租户隔离**   | Schema-per-Tenant        | -         | PostgreSQL 原生 Schema       |
| **服务治理**    | Sentinel                 | 1.8.0+    | 流控、熔断、降级                   |
| **消息队列**    | RocketMQ                 | 5.0+      | 事务消息保证最终一致性                |
| **认证授权**    | OAuth2 + JWT             | RS256     | RSA 非对称签名                  |
| **缓存**      | Redis Cluster + Caffeine | -         | 多级缓存架构                     |
| **编程语言**    | Java                     | 21        | 虚拟线程支持                     |

***

## 📦 项目结构

```
iemodo/                              ← Maven 多模块项目
├── README.md                        ← 本文档
├── pom.xml                          ← 父 POM（版本管理）
├── docker-compose.yml               ← 本地开发环境一键启动
│
├── common/                          ← 公共模块（所有服务依赖）
│   ├── pom.xml
│   └── src/main/java/com/iemodo/common/
│       ├── response/                ← 统一响应格式
│       │   ├── Response.java        ← {code, message, data, timestamp}
│       │   └── PageResponse.java    ← 分页响应
│       ├── exception/               ← 业务异常体系
│       │   ├── BusinessException.java
│       │   ├── TenantNotFoundException.java
│       │   ├── InsufficientStockException.java
│       │   └── ErrorCode.java       ← 错误码枚举
│       ├── tenant/                  ← 多租户核心实现
│       │   ├── TenantContext.java
│       │   ├── TenantIdWebFilter.java
│       │   ├── PostgresTenantConnectionFactory.java
│       │   └── TenantConnectionFactoryConfig.java
│       └── handler/
│           └── GlobalExceptionHandler.java
│
├── api-gateway/                     ← 网关服务 (Port: 8080)
│   ├── pom.xml
│   └── src/main/java/com/iemodo/gateway/
│       ├── GatewayApplication.java
│       ├── config/
│       │   ├── RouteConfig.java
│       │   ├── SentinelConfig.java
│       │   └── CorsConfig.java
│       ├── filter/
│       │   ├── JwtAuthFilter.java
│       │   ├── TenantFilter.java
│       │   └── TraceIdFilter.java
│       └── handler/
│           └── GatewayExceptionHandler.java
│
├── user-service/                    ← 用户服务 (Port: 8081)
│   ├── pom.xml
│   └── src/main/java/com/iemodo/user/
│       ├── UserServiceApplication.java
│       ├── config/
│       │   ├── MultitenantR2dbcConfig.java
│       │   ├── SecurityConfig.java
│       │   ├── JwtConfig.java
│       │   └── RedisConfig.java
│       ├── domain/
│       │   ├── User.java
│       │   ├── RefreshToken.java
│       │   └── UserDevice.java
│       ├── repository/
│       │   ├── UserRepository.java
│       │   └── RefreshTokenRepository.java
│       ├── service/
│       │   ├── AuthService.java
│       │   ├── UserService.java
│       │   └── JwtService.java
│       ├── controller/
│       │   └── AuthController.java
│       └── util/
│           └── JwtUtil.java
│
├── order-service/                   ← 订单服务 (Port: 8083)
│   ├── pom.xml
│   └── src/main/java/com/iemodo/order/
│       ├── OrderServiceApplication.java
│       ├── config/
│       ├── domain/
│       │   ├── Order.java
│       │   └── OrderItem.java
│       ├── repository/
│       ├── service/
│       │   └── OrderService.java
│       └── controller/
│           └── OrderController.java
│
├── product-service/                 ← 商品服务 (Port: 8082)
│   ├── pom.xml
│   └── src/main/java/com/iemodo/product/
│       ├── ProductServiceApplication.java
│       ├── config/
│       ├── domain/
│       │   ├── Product.java
│       │   └── SKU.java
│       ├── repository/
│       ├── service/
│       └── controller/
│
└── payment-service/                 ← 支付服务 (Port: 8085)
    ├── pom.xml
    └── src/main/java/com/iemodo/payment/
        ├── PaymentServiceApplication.java
        ├── config/
        ├── service/
        │   └── StripePaymentService.java
        └── controller/
            └── PaymentController.java
```

***

## 🚀 快速开始

### 前置条件

- **Java 21**（或更高版本）
- **Docker & Docker Compose**（用于启动基础设施）
- **Maven 3.9+**
- **Git**

### 1️⃣ 克隆项目

```bash
git clone https://github.com/sukimiya/open-iemodo.git
cd open-iemodo
```

### 2️⃣ 启动基础设施

```bash
# 启动 PostgreSQL 15 + Redis 7 + Nacos 2.3
docker compose up -d

# 验证服务状态
docker compose ps

# 查看日志
docker compose logs -f postgres
docker compose logs -f redis
docker compose logs -f nacos
```

服务地址：

- PostgreSQL: localhost:5433 (用户名: iemodo / 密码: iemodo123)
- Redis: localhost:6379
- Nacos 控制台: http://localhost:8848/nacos (nacos/nacos)

### 3️⃣ 生成 RSA 密钥对（用于 JWT）

```bash
# 在项目根目录执行
mkdir -p user-service/src/main/resources/jwt
mkdir -p api-gateway/src/main/resources/jwt

# 生成 2048-bit RSA 密钥对
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem

# 复制到各服务
cp private.pem user-service/src/main/resources/jwt/
cp public.pem user-service/src/main/resources/jwt/
cp public.pem api-gateway/src/main/resources/jwt/

# 清理临时文件
rm private.pem public.pem
```

### 4️⃣ 启动微服务

```bash
# 在三个不同的终端窗口中启动以下服务

# 终端 1: 用户服务
JAVA_HOME=/usr/libexec/java_home -V 2>&1 | grep 21
mvn spring-boot:run -pl user-service

# 终端 2: 订单服务
mvn spring-boot:run -pl order-service

# 终端 3: API 网关
mvn spring-boot:run -pl api-gateway
```

### 5️⃣ 验证服务

```bash
# 1. 用户注册
curl -X POST http://localhost:8080/uc/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -H "X-TenantID: tenant_001" \
  -d '{
    "email":"test@example.com",
    "password":"Test1234!",
    "displayName":"Test User"
  }'

# 预期响应:
# {"code":200,"message":"success","data":{"accessToken":"eyJ...","refreshToken":"..."},"timestamp":"2026-03-21T10:30:00Z"}

# 2. 用户登录
curl -X POST http://localhost:8080/uc/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-TenantID: tenant_001" \
  -d '{
    "email":"test@example.com",
    "password":"Test1234!"
  }'

# 3. 初始化库存（在 Redis 中）
redis-cli -a redis123 SET stock:tenant_001:SKU-001 100

# 4. 创建订单
curl -X POST http://localhost:8080/oc/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "X-TenantID: tenant_001" \
  -H "Authorization: Bearer <access_token>" \
  -d '{
    "customerId": 1,
    "currency": "USD",
    "items": [
      {
        "productId": 1,
        "sku": "SKU-001",
        "quantity": 2,
        "unitPrice": 19.99
      }
    ]
  }'
```

***

## 📋 已实现功能

### ✅ 第一期 MVP (完成)

**基础设施**
- Nacos 服务注册与配置中心
- Spring Cloud Gateway 网关（带 Sentinel 限流）
- 多租户路由基础架构（Schema-per-Tenant）
- Docker Compose 一键启动

**用户服务**
- 邮箱 + 密码注册/登录
- OAuth2 认证框架（预留 Google/微信社交登录）
- JWT Token 生成与校验（RS256 非对称签名）
- 刷新令牌机制（30 天有效期）
- Token 黑名单管理（Redis）
- 多设备登录支持

**订单服务**
- 创建订单（自动生成订单号）
- 订单状态机（PENDING_PAYMENT → PAID → PROCESSING → SHIPPED → DELIVERED）
- Redis Lua 原子库存预扣（防止超卖）
- 订单查询与列表（分页）
- 取消订单（库存回滚）

**通用基础设施**
- 统一响应格式
- 全局异常处理（对应错误码）
- 链路追踪（TraceID）
- Flyway 数据库版本管理
- 多租户 R2DBC 数据源路由

## 🔄 第二期规划

| 功能           | 说明                       | 优先级 |
| -------------- | -------------------------- | ------ |
| 商品服务       | 商品 CRUD、SKU 管理、库存初始化 | P0     |
| 支付服务       | Stripe 集成、支付回调处理     | P0     |
| 库存服务       | 独立库存管理、异步扣减确认     | P1     |
| 多语言/多币种  | 国际化支持                  | P2     |
| 物流/WMS       | 订单配送流程                | P2     |
| 风控/反欺诈    | 交易监控引擎                | P3     |

***

## 🏛️ 核心架构设计

### 多租户隔离策略

采用 Schema-per-Tenant 方案：

```
PostgreSQL 实例
├─ Schema: schema_tenant_001
│   ├── users
│   ├── orders
│   └── order_items
├─ Schema: schema_tenant_002
│   ├── users
│   ├── orders
│   └── order_items
└─ Schema: public
    └── 系统配置表（共享）
```

租户识别链路：

```
HTTP Request (X-TenantID: tenant_001)
    ↓
TenantIdWebFilter
    ↓ contextWrite(ctx → ctx.put(TENANT_ID, "tenant_001"))
Reactor Context
    ↓
PostgresTenantConnectionFactory.determineCurrentLookupKey()
    ↓ Mono.deferContextual → ctx.get(TENANT_ID)
路由到 ConnectionFactory("tenant_001")
    ↓ 连接到 PostgreSQL 的 schema_tenant_001
```

### 响应式全链路

遵循 Spring WebFlux 最佳实践：

```java
// ❌ 错误做法（阻塞）
public User getUser(Long userId) {
    return userRepository.findById(userId);  // 同步 JDBC
}

// ✅ 正确做法（非阻塞）
public Mono<User> getUser(Long userId) {
    return userRepository.findById(userId);  // R2DBC 返回 Mono
}

// ✅ 链式操作
public Mono<OrderDTO> createOrder(Mono<CreateOrderRequest> request) {
    return request
        .flatMap(req -> inventoryService.preDeduct(req.getSku(), req.getQuantity()))
        .flatMap(success -> orderService.saveOrder(...))
        .map(order -> orderMapper.toDTO(order));
}
```

### 缓存架构

多级缓存递进：

```
┌──────────────────┐
│  L1 (Caffeine)   │  ← 热点数据（5分钟TTL）
│  热销商品、配置   │
└────────┬─────────┘
         │ 未命中
┌────────▼─────────┐
│  L2 (Redis)      │  ← 常用数据（30分钟TTL）
│  商品详情、库存   │
└────────┬─────────┘
         │ 未命中
┌────────▼─────────┐
│  L3 (PostgreSQL) │  ← 完整数据
│  持久化存储       │
└──────────────────┘
```

### 库存扣减流程

使用 Redis Lua 脚本保证原子性，防止超卖：

```lua
-- 库存扣减 Lua 脚本（原子操作）
local stock = tonumber(redis.call('get', KEYS[1]));
if stock == nil then
    return -1;  -- key 不存在
end
if stock >= tonumber(ARGV[1]) then
    redis.call('decrby', KEYS[1], ARGV[1]);
    return 1;   -- 成功
else
    return 0;   -- 库存不足
end
```

***

## 📊 API 接口规范

### 请求头规范

| Header        | 必填 | 说明                              |
| ------------- | ---- | --------------------------------- |
| X-TenantID    | 是   | 租户标识（路由到对应 Schema）      |
| Authorization | 否   | Bearer <JWT_TOKEN>（认证端点除外） |
| X-Request-ID  | 否   | 请求追踪 ID（网关自动生成）        |
| Content-Type  | 是   | application/json                  |

### 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 业务数据
  },
  "timestamp": "2026-03-21T10:30:00Z"
}
```

### 错误码定义

| 错误码 | HTTP 状态 | 含义                      |
| ------ | --------- | ------------------------- |
| 200    | 200       | 成功                      |
| 400    | 400       | 参数错误                  |
| 401    | 401       | 未授权（缺少/无效 JWT）    |
| 403    | 403       | 禁止访问                  |
| 404    | 404       | 资源不存在                |
| 409    | 409       | 库存不足                  |
| 4001   | 400       | 租户不存在                |
| 500    | 500       | 系统错误                  |

### 核心 API 端点

#### 认证模块

```http
# 用户注册
POST /uc/api/v1/auth/register
Content-Type: application/json
X-TenantID: tenant_001

{
  "email": "user@example.com",
  "password": "Secure1234!",
  "displayName": "User Name"
}

# 响应 (200 OK)
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 10001,
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 3600
  }
}

# 用户登录
POST /uc/api/v1/auth/login
Content-Type: application/json
X-TenantID: tenant_001

{
  "email": "user@example.com",
  "password": "Secure1234!"
}

# 获取当前用户
GET /uc/api/v1/users/me
Authorization: Bearer <access_token>
X-TenantID: tenant_001

# 登出
POST /uc/api/v1/auth/logout
Authorization: Bearer <access_token>
X-TenantID: tenant_001
```

#### 订单模块

```http
# 创建订单
POST /oc/api/v1/orders
Content-Type: application/json
Authorization: Bearer <access_token>
X-TenantID: tenant_001

{
  "customerId": 10001,
  "currency": "USD",
  "shippingAddress": {
    "name": "John Doe",
    "phone": "+1234567890",
    "address": "123 Main St, New York"
  },
  "items": [
    {
      "productId": 1001,
      "sku": "SKU-001",
      "quantity": 2,
      "unitPrice": 19.99
    }
  ]
}

# 获取订单详情
GET /oc/api/v1/orders/{orderId}
Authorization: Bearer <access_token>
X-TenantID: tenant_001

# 查询订单列表（分页）
GET /oc/api/v1/orders?page=1&pageSize=20&status=PAID
Authorization: Bearer <access_token>
X-TenantID: tenant_001

# 取消订单
POST /oc/api/v1/orders/{orderId}/cancel
Authorization: Bearer <access_token>
X-TenantID: tenant_001
```

***

## 🧪 测试

### 单元测试

```bash
# 运行所有测试
mvn clean test

# 运行特定模块的测试
mvn clean test -pl user-service
mvn clean test -pl order-service

# 运行特定测试类
mvn clean test -Dtest=AuthServiceTest
```

### 集成测试（TestContainers）

项目使用 TestContainers 进行集成测试，自动启动真实 PostgreSQL 和 Redis：

```java
@IntegrationTest
class AuthServiceIntegrationTest {
    
    @Test
    void shouldRegisterAndLoginUser() {
        // 使用真实容器验证完整流程
        User user = authService.register("test@example.com", "password").block();
        Mono<TokenResponse> result = authService.login("test@example.com", "password");
        
        StepVerifier.create(result)
            .assertNext(token -> assertThat(token.getAccessToken()).isNotBlank())
            .verifyComplete();
    }
}
```

### 冒烟测试脚本

```bash
# 启动所有服务后运行
bash scripts/smoke-test.sh
```

***

## 📈 性能指标

| 指标            | 目标             | 当前      |
| --------------- | ---------------- | --------- |
| 单节点 QPS      | 1000+            | ✅        |
| 平均延迟        | < 200ms (P50)    | ✅        |
| P95 延迟        | < 500ms          | ✅        |
| 可用性          | 99.9%            | ✅ (本地) |
| Redis 命中率    | > 95%            | 配置依赖  |
| 数据库连接池    | 20 (共享)        | ✅        |

***

## 🔒 安全特性

### 认证与授权

- ✅ **OAuth2 + JWT**：RS256 非对称签名，公钥可分发给下游服务独立验证
- ✅ **多因素认证**：预留 OTP/WebAuthn 支持
- ✅ **Token 黑名单**：Redis 存储已登出 Token
- ✅ **HTTPS Only**：生产环境强制 HTTPS
- ✅ **密码安全**：BCrypt 加密（strength=12）

### 数据隐私

- ✅ **Schema 隔离**：租户数据物理分离
- ✅ **SQL 参数化**：防止注入攻击
- ✅ **敏感字段加密**：如 OAuth2 access_token
- ✅ **审计日志**：完整记录关键操作

### 合规性

- ✅ **GDPR 支持**：用户数据导出、删除功能
- ✅ **CCPA 支持**：加州隐私数据处理
- ✅ **PCI DSS**：支付数据安全（与 Stripe 集成）

***

## 📚 文档

- [架构决策文档](./docs/adr) - 详细的技术决策和设计权衡
- [实现步骤](./docs/implementation.md) - 四阶段详细实现指南
- [待办清单](./docs/todo.md) - 项目进度和 checklist
- [PRD 文档](./docs/prd.md) - 完整产品需求文档
- [多租户调研](./docs/multitenant-research.md) - SaaS 多租户技术调研

***

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发流程

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交代码 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

### 代码规范

- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 所有方法返回 `Mono<T>` 或 `Flux<T>`（禁止阻塞调用）
- 使用构造函数注入（Spring 推荐）
- 100% 单元测试覆盖率（核心业务逻辑）

***

## 📝 许可证

本项目采用 Apache License 2.0 开源许可证。详见 [LICENSE](./LICENSE) 文件。

***

## 💬 常见问题

**Q: 为什么选择 WebFlux？**

A: WebFlux 采用响应式编程模型，相比传统 Spring MVC：

- **高并发**：少量线程处理大量并发连接
- **低延迟**：异步非阻塞 I/O
- **内存效率**：更低的内存占用，适合容器化部署

**Q: Schema-per-Tenant 如何处理跨租户查询？**

A: Schema-per-Tenant 主要用于数据隔离，大多数查询均为单租户。对于统计分析等跨租户需求，可：

1. 使用 Elasticsearch 等搜索引擎（已索引多租户数据）
2. 创建数据仓库（定时从各 Schema 聚合数据）
3. 使用 Postgres 的 postgres_fdw 外表查询

**Q: 如何添加新的微服务？**

A: 参考以下步骤：

1. 在 pom.xml 中添加模块
2. 复制 order-service 的结构，修改包名和业务逻辑
3. 在 docker-compose.yml 中添加服务（如需）
4. 在 API Gateway 的 application.yml 中添加路由规则

**Q: 生产环境如何部署？**

A: 参考 Kubernetes 部署指南（规划中）：

- 使用 Helm Chart 部署
- AWS RDS 替代本地 PostgreSQL
- AWS ElastiCache 替代本地 Redis
- 多可用区高可用架构

***

## 📞 联系方式

- **GitHub Issues**: [项目 Issue 列表](https://github.com/sukimiya/open-iemodo/issues)
- **讨论**: [GitHub Discussions](https://github.com/sukimiya/open-iemodo/discussions)
- **邮件**: [项目维护者](mailto:your-email@example.com)

***

## 🙏 致谢

感谢以下开源项目的支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud Alibaba](https://sca.aliyun.com/)
- [Project Reactor](https://projectreactor.io/)
- [Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc)

⭐ 如果这个项目对你有帮助，请给一个 Star！
