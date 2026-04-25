# RULE.md — Claude Code 项目指南

> 本文档供 Claude Code 读取，用于保持跨会话的上下文一致性。
> 修改 README.md 时请同步更新本文档中的关键信息。

## 项目概述

- **项目**: Avacha (iemodo) — 跨境电商系统，Java 21 + Spring Boot 3.3 + WebFlux + R2DBC
- **多租户**: Schema-per-Tenant (PostgreSQL)
- **构建工具**: Maven 多模块

## 分支说明

### main 分支 (默认)
- 完整的 16 微服务架构
- 基础设施: Spring Cloud Alibaba (Nacos + Gateway + Sentinel)
- Docker: PostgreSQL 15 + Redis 7 + Nacos 2.3

### iemodo-lite 分支
- 单 JAR 单体版本，移除 Nacos
- 新增 `app-boot` 模块作为统一入口
- Gateway 功能迁移到 app-boot 的 WebFilter (JWT 校验、CORS)
- Docker: 仅 PostgreSQL + Redis
- 构建: `mvn package -pl app-boot -am -Dmaven.test.skip=true`
- 运行: `java -jar app-boot/target/app-boot-1.0.0-SNAPSHOT.jar`

## 重要记忆

### 为什么有 iemodo-lite
因为微服务版需要 ~5GB+ 内存（16个 Java 进程 + Nacos），不适合小型部署。
lite 版将 16 个服务合并为 1 个 JAR，只需 ~1.5GB。

### 改动要点 (2026-04-25)
- `app-boot/` 模块 — 单体入口
- 所有服务：移除 @SpringBootApplication、Nacos 依赖、Nacos 配置
- 统一 R2DBC Repository 扫描 (LiteR2dbcConfig)
- 统一 application.yml，无 Nacos

### 已知问题
- 测试文件中有编译错误 (order-service OrderServiceTest 构造参数不匹配)，需 `-Dmaven.test.skip=true`
- 部分外部服务为 mock/占位符（map-service 坐标硬编码、notification 通道未对接真实服务商）
- Flyway schema 配置在各服务 application.yml 中，单体版本需统一管理

### 服务模块清单 (16 个)
common, api-gateway, user-service(8081), product-service(8082), order-service(8083),
inventory-service(8084), payment-service(8085), pricing-service(8086), map-service(8087),
tax-service(8088), marketing-service(8089), file-service(8090),
tenant-management-service(8091), fulfillment-service(8092), notification-service(8093),
rma-service(8095), review-service(8096)

### 关键配置
- PostgreSQL: localhost:5433, iemodo/iemodo123
- Redis: localhost:6379, redis123
- 多租户: schema_tenant_001, schema_tenant_002
- JWT: RS256, classpath:jwt/private.pem + jwt/public.pem

## 记忆目录
项目记忆存放在 `docs/remind/` 目录中，命名格式为 `主题-描述.md`。
