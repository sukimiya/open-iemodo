<br />

# Avacha · iemodo
## 跨境电商微服务系统

![Java](https://img.shields.io/badge/Java-21-green?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen?logo=spring)
![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0-blue)
![WebFlux](https://img.shields.io/badge/WebFlux-Reactive-brightblue)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

**一个面向全球、支持多租户的开源跨境电商系统，采用响应式架构、云原生设计。**

> 本项目提供两种部署模式：
> - **`main` 分支** — 完整 16 微服务架构（Spring Cloud Alibaba + Nacos）
> - **`iemodo-lite` 分支** — 单 JAR 单体版本（极致省资源）

***

## 📋 项目概述

**Avacha** 是一个基于 Spring Cloud Alibaba 和 Spring WebFlux 的**企业级跨境电商平台**（技术代号：iemodo），专为全球商家提供多店铺管理、订单处理、支付集成等核心功能。该项目采用**全链路响应式架构**和**Schema-per-Tenant 多租户隔离方案**，支持高并发场景（单节点 1000+ QPS）。

### 核心特性

✅ **全链路响应式**：基于 Project Reactor（Mono/Flux），完全非阻塞 I/O  
✅ **多租户 SaaS**：Schema-per-Tenant 隔离 + 独立数据库可选  
✅ **微服务架构**：Spring Cloud Alibaba（Nacos + Gateway + Sentinel）  
✅ **海外部署**：AWS RDS + ElastiCache + S3，满足 GDPR/CCPA 合规  
✅ **生产就绪**：完整的认证体系、审计日志、监控告警

***

## 🔀 分支说明

### main 分支（微服务版）

完整的 16 微服务架构，适用于中大型部署。

```
16 个微服务 + 3 个基础设施 (PostgreSQL + Redis + Nacos)
内存需求: ~5GB+
```

详见 [README](./README.MD)（当前文档）。

### iemodo-lite 分支（单体极简版）

将所有服务合并为单个 JAR，移除 Nacos，适用于小型化部署和开发环境。

```
1 个 JAR + 2 个基础设施 (PostgreSQL + Redis)
内存需求: ~1.5GB
一台 1C2G 的轻量云服务器即可运行
```

**构建运行：**
```bash
git checkout iemodo-lite
mvn package -pl app-boot -am -Dmaven.test.skip=true
java -jar app-boot/target/app-boot-1.0.0-SNAPSHOT.jar
```

***

## 🏗️ 技术架构（微服务版）

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
├── RULE.md                          ← Claude Code 会话指南
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
│       │   └── MultitenantR2dbcConfiguration.java
│       ├── entity/                  ← 基础实体(BaseEntity)
│       ├── db/                      ← 慢查询熔断器
│       ├── cache/                   ← 缓存工具
│       ├── audit/                   ← 审计日志
│       ├── config/                  ← 通用配置(OpenAPI)
│       ├── context/                 ← Reactor 上下文
│       ├── filter/                  ← 日志 WebFilter
│       └── handler/                 ← 全局异常处理
│
├── app-boot/                        ← [iemodo-lite] 单体入口（仅 lite 分支）
│   ├── pom.xml
│   └── src/main/java/com/iemodo/
│       ├── IemodoApplication.java
│       └── config/
│           ├── JwtWebFilter.java
│           ├── LiteCorsConfig.java
│           ├── LiteGatewayProperties.java
│           └── LiteR2dbcConfig.java
│
├── api-gateway/                     ← 网关服务 (Port: 8080)
├── user-service/                    ← 用户服务 (Port: 8081)
├── product-service/                 ← 商品服务 (Port: 8082)
├── order-service/                   ← 订单服务 (Port: 8083)
├── inventory-service/               ← 库存服务 (Port: 8084)
├── payment-service/                 ← 支付服务 (Port: 8085)
├── pricing-service/                 ← 定价服务 (Port: 8086)
├── map-service/                     ← 地图服务 (Port: 8087)
├── tax-service/                     ← 税务服务 (Port: 8088)
├── marketing-service/               ← 营销服务 (Port: 8089)
├── file-service/                    ← 文件服务 (Port: 8090)
├── tenant-management-service/       ← 租户管理 (Port: 8091)
├── fulfillment-service/             ← 履约服务 (Port: 8092)
├── rma-service/                     ← 售后/RMA (Port: 8095)
├── review-service/                  ← 评价服务 (Port: 8096)
├── notification-service/            ← 通知服务 (Port: 8093)
│
├── docs/                            ← 文档
│   ├── remind/                      ← 项目记忆（Claude 自动维护）
│   └── plan/                        ← 实施计划
├── scripts/                         ← 脚本
└── Dockerfile                       ← [iemodo-lite] 单体 Dockerfile
```

***

## 🚀 快速开始

### 微服务版（main 分支）

```bash
git checkout main

# 启动基础设施
docker compose up -d

# 生成 JWT 密钥对
mkdir -p user-service/src/main/resources/jwt
mkdir -p api-gateway/src/main/resources/jwt
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
cp private.pem user-service/src/main/resources/jwt/
cp public.pem user-service/src/main/resources/jwt/
cp public.pem api-gateway/src/main/resources/jwt/

# 启动服务
mvn spring-boot:run -pl user-service
mvn spring-boot:run -pl order-service
mvn spring-boot:run -pl api-gateway
```

### 单体版（iemodo-lite 分支）

```bash
git checkout iemodo-lite

# 启动基础设施（仅 PostgreSQL + Redis，无需 Nacos）
docker compose up -d

# 生成 JWT 密钥对
mkdir -p app-boot/src/main/resources/jwt
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
cp private.pem user-service/src/main/resources/jwt/
cp public.pem user-service/src/main/resources/jwt/
cp public.pem api-gateway/src/main/resources/jwt/
cp private.pem app-boot/src/main/resources/jwt/
cp public.pem app-boot/src/main/resources/jwt/

# 构建并运行
mvn package -pl app-boot -am -Dmaven.test.skip=true
java -jar app-boot/target/app-boot-1.0.0-SNAPSHOT.jar
```

### 验证服务

```bash
# 用户注册
curl -X POST http://localhost:8080/uc/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -H "X-TenantID: tenant_001" \
  -d '{"email":"test@example.com","password":"Test1234!","displayName":"Test User"}'
```

***

## 📋 功能清单

### ✅ 已完成

**基础设施**
- Nacos 服务注册与配置中心（微服务版）
- Spring Cloud Gateway 网关（带 Sentinel 限流）
- 多租户路由基础架构（Schema-per-Tenant）
- Docker Compose 一键启动
- 慢查询熔断器
- TraceID 链路追踪

**用户服务**
- 邮箱 + 密码注册/登录
- OAuth2 认证框架（Google 社交登录预留）
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
- 乐观锁 + 幂等令牌
- 支付超时自动关单

**商品服务**
- 商品 CRUD 和 SKU 管理
- 分类和品牌管理
- 多国家可见性配置

**库存服务**
- 多仓库库存管理
- Redis + DB 双重防超卖
- 库存事务记录

**支付服务**
- Stripe 支付集成框架
- 支付回调解耦与幂等性
- 定时对账任务
- 退款处理

**定价服务**
- 区域定价 + 货币转换
- 阶梯折扣 + 客户段定价
- PricingGuard 异常检测

**税务服务**
- VAT/GST/美国销售税/日本消费税
- B2B 免税检查

**其他服务**
- 营销服务（优惠券管理）
- 履约服务（仓库分配评分）
- 评价服务（审核工作流）
- 售后/RMA 服务（区域规则）
- 通知服务（邮件/SMS/推送框架）
- 文件服务（MinIO 集成）
- 租户管理（Schema 自动创建）
- 地图服务（定位/距离计算框架）

### 🔄 规划中

| 功能 | 说明 | 优先级 |
| --- | --- | --- |
| 商品服务完善 | 商品 CRUD、SKU 管理、库存初始化 | P0 |
| 支付服务完善 | Stripe 集成、支付回调处理 | P0 |
| 库存服务完善 | 独立库存管理、异步扣减确认 | P1 |
| 多语言/多币种 | 国际化支持 | P2 |
| 物流/WMS | 订单配送流程 | P2 |
| 风控/反欺诈 | 交易监控引擎 | P3 |

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

***

## 📊 API 接口规范

### 请求头规范

| Header | 必填 | 说明 |
| --- | --- | --- |
| X-TenantID | 是 | 租户标识（路由到对应 Schema） |
| Authorization | 否 | Bearer \<JWT_TOKEN\>（认证端点除外） |
| X-Request-ID | 否 | 请求追踪 ID（网关自动生成） |
| Content-Type | 是 | application/json |

### 核心 API 端点

详见 [docs/api](./docs) 目录。

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

***

## 📝 许可证

本项目采用 Apache License 2.0 开源许可证。详见 [LICENSE](./LICENSE) 文件。

***

## 🙏 致谢

感谢以下开源项目的支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud Alibaba](https://sca.aliyun.com/)
- [Project Reactor](https://projectreactor.io/)
- [Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc)

⭐ 如果这个项目对你有帮助，请给一个 Star！
