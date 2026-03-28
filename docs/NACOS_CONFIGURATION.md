# Nacos 配置清单

**Group**: IEMODO  
**格式**: YAML  
**命名空间**: public (或 dev/prod)

---

## 1. Data ID: `payment-service.yaml`

```yaml
# Payment Service 配置

# Stripe 支付配置（生产环境必须替换）
stripe:
  public-key: ${STRIPE_PUBLIC_KEY:pk_test_placeholder}
  secret-key: ${STRIPE_SECRET_KEY:sk_test_placeholder}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET:whsec_placeholder}

# 多租户数据库配置
iemodo:
  tenants:
    - id: tenant_001
      type: SCHEMA
      schema: payment_tenant_001
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5432}
      database: ${DB_NAME:iemodo_platform}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}
    - id: tenant_002
      type: SCHEMA
      schema: payment_tenant_002
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5432}
      database: ${DB_NAME:iemodo_platform}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}

# Spring 配置
spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:iemodo_platform}
    username: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:iemodo_platform}
    user: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    schemas: payment_tenant_001,payment_tenant_002
    locations: classpath:db/migration
    baseline-on-migrate: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:redis123}
      database: 0
  cloud:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
      discovery:
        enabled: true
        namespace: public
        group: IEMODO
      config:
        enabled: true
        namespace: public
        group: IEMODO
        file-extension: yaml

# 日志配置
logging:
  level:
    com.iemodo.payment: DEBUG
    org.springframework.data.r2dbc: WARN
```

---

## 2. Data ID: `product-service.yaml`

```yaml
# Product Service 配置

# 商品服务业务配置
iemodo:
  product:
    cache:
      product-ttl: 3600      # 商品缓存TTL(秒)
      category-ttl: 7200     # 分类缓存TTL(秒)
    search:
      default-page-size: 20
      max-page-size: 100
  # 多租户数据库配置
  tenants:
    - id: tenant_001
      type: SCHEMA
      schema: product_tenant_001
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5432}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}
    - id: tenant_002
      type: SCHEMA
      schema: product_tenant_002
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5432}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}

# Spring 配置
spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:iemodo}
    username: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    pool:
      max-size: 10
      initial-size: 2
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:iemodo}
    user: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    schemas: product_tenant_001,product_tenant_002
    locations: classpath:db/migration
    baseline-on-migrate: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:redis123}
      lettuce:
        pool:
          max-active: 8
          max-idle: 4
  cloud:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
      discovery:
        enabled: true
        namespace: public
        group: IEMODO
      config:
        enabled: true
        namespace: public
        group: IEMODO
        file-extension: yaml

# 日志配置
logging:
  level:
    com.iemodo.product: INFO
    org.springframework.data.r2dbc: WARN
```

---

## 3. Data ID: `inventory-service.yaml`

```yaml
# Inventory Service 配置

# 库存服务业务配置
iemodo:
  inventory:
    cache:
      stock-ttl: 86400        # 库存缓存TTL(24小时)
    allocation:
      max-distance-km: 5000   # 最大仓库距离
      default-preference: BALANCED  # 默认分配策略: COST/DISTANCE/SPEED/BALANCED
  # 多租户数据库配置
  tenants:
    - id: tenant_001
      type: SCHEMA
      schema: inventory_tenant_001
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5432}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}
    - id: tenant_002
      type: SCHEMA
      schema: inventory_tenant_002
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5432}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}

# Spring 配置
spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:iemodo}
    username: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    pool:
      max-size: 10
      initial-size: 2
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:iemodo}
    user: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    schemas: inventory_tenant_001,inventory_tenant_002
    locations: classpath:db/migration
    baseline-on-migrate: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:redis123}
      lettuce:
        pool:
          max-active: 8
          max-idle: 4
  cloud:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
      discovery:
        enabled: true
        namespace: public
        group: IEMODO
      config:
        enabled: true
        namespace: public
        group: IEMODO
        file-extension: yaml

# 日志配置
logging:
  level:
    com.iemodo.inventory: INFO
    org.springframework.data.r2dbc: WARN
```

---

## 4. Data ID: `file-service.yaml`

```yaml
# File Service 配置

# MinIO 配置
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  url-expiry: 3600  # 预签名URL过期时间(秒)

# Spring 配置
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 100MB
  cloud:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
      discovery:
        enabled: true
        namespace: public
        group: IEMODO
      config:
        enabled: true
        namespace: public
        group: IEMODO
        file-extension: yaml

# 日志配置
logging:
  level:
    com.iemodo.file: INFO
    io.minio: WARN
```

---

## 5. Data ID: `tenant-management.yaml`

```yaml
# Tenant Management Service 配置

# 多租户元数据库配置（使用独立 schema）
iemodo:
  tenants:
    - id: platform
      type: SCHEMA
      schema: tenant_meta
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5432}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}

# Spring 配置
spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:iemodo}?schema=tenant_meta
    username: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:iemodo}?currentSchema=tenant_meta
    user: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    schemas: tenant_meta
    locations: classpath:db/migration
    baseline-on-migrate: true
  cloud:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
      discovery:
        enabled: true
        namespace: public
        group: IEMODO
      config:
        enabled: true
        namespace: public
        group: IEMODO
        file-extension: yaml

# 日志配置
logging:
  level:
    com.iemodo.tenant: INFO
    org.springframework.data.r2dbc: WARN
```

---

## 6. Data ID: `api-gateway.yaml` (已存在，需要更新)

```yaml
# API Gateway 配置

iemodo:
  gateway:
    jwt:
      # RSA 公钥路径（用于验签）
      public-key-path: ${JWT_PUBLIC_KEY_PATH:classpath:jwt/public.pem}
    # JWT 免校验路径
    whitelist:
      - POST:/uc/api/v1/auth/register
      - POST:/uc/api/v1/auth/login
      - POST:/uc/api/v1/auth/refresh
      - GET:/uc/api/v1/auth/oauth2/**
      - POST:/pay/api/v1/webhooks/stripe
      - GET:/actuator/**
      - GET:/health

# Spring 配置
spring:
  cloud:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
      discovery:
        namespace: ${NACOS_NAMESPACE:}
        group: IEMODO
        register-enabled: true
      config:
        namespace: ${NACOS_NAMESPACE:}
        group: IEMODO
        file-extension: yaml
    gateway:
      discovery:
        locator:
          enabled: false
      routes:
        # User Service
        - id: user-service-route
          uri: lb://user-service
          predicates:
            - Path=/uc/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
                redis-rate-limiter.requestedTokens: 1
        # Order Service
        - id: order-service-route
          uri: lb://order-service
          predicates:
            - Path=/oc/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 200
                redis-rate-limiter.burstCapacity: 400
                redis-rate-limiter.requestedTokens: 1
        # Product Service
        - id: product-service-route
          uri: lb://product-service
          predicates:
            - Path=/pc/**
        # Inventory Service
        - id: inventory-service-route
          uri: lb://inventory-service
          predicates:
            - Path=/inv/**
        # Payment Service
        - id: payment-service-route
          uri: lb://payment-service
          predicates:
            - Path=/pay/**
        # File Service
        - id: file-service-route
          uri: lb://file-service
          predicates:
            - Path=/fs/**
        # Tenant Management
        - id: tenant-management-route
          uri: lb://tenant-management-service
          predicates:
            - Path=/tm/**
    sentinel:
      transport:
        dashboard: ${SENTINEL_DASHBOARD:localhost:8858}
        port: 8719
      eager: true
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:iemodo}?schema=gateway_config
    username: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    pool:
      max-size: 10
      initial-size: 5
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:redis123}
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:iemodo}?currentSchema=gateway_config
    user: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    schemas: gateway_config
    locations: classpath:db/migration
    baseline-on-migrate: true

# 日志配置
logging:
  level:
    com.iemodo: DEBUG
    org.springframework.cloud.gateway: INFO
    com.alibaba.cloud.sentinel: INFO
```

---

## 7. Data ID: `user-service.yaml` (已存在，建议更新)

```yaml
# User Service 配置

# OAuth2 配置
google:
  oauth2:
    client-id: ${GOOGLE_CLIENT_ID:1096869572094-k9d2oqrj4h3i07728mmo78cu4henhj69.apps.googleusercontent.com}
    client-secret: ${GOOGLE_CLIENT_SECRET:replace-with-real-secret}

# JWT 配置
iemodo:
  user:
    jwt:
      private-key-path: ${JWT_PRIVATE_KEY_PATH:classpath:jwt/private.pem}
      public-key-path: ${JWT_PUBLIC_KEY_PATH:classpath:jwt/public.pem}
      access-token-ttl-minutes: 60
      refresh-token-ttl-days: 30
      issuer: iemodo-user-service
  # 多租户数据库配置
  tenants:
    - id: tenant_001
      type: SCHEMA
      schema: schema_tenant_001
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5432}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}
    - id: tenant_002
      type: SCHEMA
      schema: schema_tenant_002
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5432}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}

# Spring 配置
spring:
  flyway:
    enabled: true
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:iemodo}
    user: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    schemas: schema_tenant_001,schema_tenant_002
    locations: classpath:db/migration
    baseline-on-migrate: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:redis123}
  cloud:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
      discovery:
        group: IEMODO
      config:
        group: IEMODO
        file-extension: yaml

# 日志配置
logging:
  level:
    com.iemodo: DEBUG
    org.springframework.security: INFO
    org.flywaydb: INFO
```

---

## 8. Data ID: `order-service.yaml` (已存在，建议更新)

```yaml
# Order Service 配置

iemodo:
  # 多租户数据库配置
  tenants:
    - id: tenant_001
      type: SCHEMA
      schema: schema_tenant_001
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5432}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}
    - id: tenant_002
      type: SCHEMA
      schema: schema_tenant_002
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5432}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}

# Spring 配置
spring:
  flyway:
    enabled: true
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:iemodo}
    user: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    schemas: schema_tenant_001,schema_tenant_002
    locations: classpath:db/migration
    baseline-on-migrate: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:redis123}
  cloud:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
      discovery:
        group: IEMODO
      config:
        group: IEMODO
        file-extension: yaml

# 日志配置
logging:
  level:
    com.iemodo: DEBUG
    org.flywaydb: INFO
```

---

## 📝 配置说明

### 环境变量说明

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DB_HOST` | PostgreSQL 主机 | localhost |
| `DB_PORT` | PostgreSQL 端口 | 5432 |
| `DB_NAME` | 数据库名 | iemodo / iemodo_platform |
| `DB_USER` | 数据库用户名 | iemodo |
| `DB_PASSWORD` | 数据库密码 | iemodo123 |
| `REDIS_HOST` | Redis 主机 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `REDIS_PASSWORD` | Redis 密码 | redis123 |
| `NACOS_SERVER_ADDR` | Nacos 地址 | localhost:8848 |
| `STRIPE_PUBLIC_KEY` | Stripe 公钥 | - |
| `STRIPE_SECRET_KEY` | Stripe 私钥 | - |
| `STRIPE_WEBHOOK_SECRET` | Stripe Webhook 密钥 | - |
| `MINIO_ENDPOINT` | MinIO 地址 | http://localhost:9000 |
| `MINIO_ACCESS_KEY` | MinIO Access Key | minioadmin |
| `MINIO_SECRET_KEY` | MinIO Secret Key | minioadmin |

### 多租户 Schema 命名规则

| 服务 | Tenant 001 Schema | Tenant 002 Schema |
|------|-------------------|-------------------|
| User/Order | schema_tenant_001 | schema_tenant_002 |
| Product | product_tenant_001 | product_tenant_002 |
| Inventory | inventory_tenant_001 | inventory_tenant_002 |
| Payment | payment_tenant_001 | payment_tenant_002 |

---

## 🚀 配置步骤

1. 访问 Nacos 控制台: http://localhost:8848/nacos (nacos/nacos)
2. 配置管理 → 配置列表
3. 点击 "+" 创建配置
4. Data ID: 如 `payment-service.yaml`
5. Group: `IEMODO`
6. 格式: `YAML`
7. 粘贴对应配置内容
8. 发布

---

## ⚠️ 生产环境注意事项

1. **Stripe 密钥**: 必须替换为真实的生产密钥
2. **数据库密码**: 使用强密码，避免默认值
3. **Redis 密码**: 必须设置
4. **JWT 密钥**: 使用 RSA 2048 位密钥对
5. **MinIO 密钥**: 使用强密码
6. **Google OAuth2**: Client ID 已配置 (1096869572094-...)，**Client Secret 需要手动配置**，切勿提交到代码仓库
