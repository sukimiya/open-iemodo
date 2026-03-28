#!/bin/bash

# Nacos 配置导入脚本
# 使用方式: ./import-nacos-configs.sh [nacos地址] [用户名] [密码]
# 示例: ./import-nacos-configs.sh http://localhost:8848 nacos nacos

set -e

NACOS_URL=${1:-"http://localhost:8848"}
NACOS_USER=${2:-"nacos"}
NACOS_PASS=${3:-"nacos"}
GROUP="IEMODO"

echo "======================================"
echo "Nacos 配置导入工具"
echo "======================================"
echo "Nacos地址: $NACOS_URL"
echo "Group: $GROUP"
echo "======================================"
echo ""

# 临时目录存放配置文件
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# 创建 payment-service.yaml
cat > $TEMP_DIR/payment-service.yaml << 'EOF'
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
      port: ${DB_PORT:5433}
      database: ${DB_NAME:iemodo_platform}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}
    - id: tenant_002
      type: SCHEMA
      schema: payment_tenant_002
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5433}
      database: ${DB_NAME:iemodo_platform}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}

# Spring 配置
spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo_platform}
    username: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo_platform}
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
EOF

# 创建 product-service.yaml
cat > $TEMP_DIR/product-service.yaml << 'EOF'
# Product Service 配置

# 商品服务业务配置
iemodo:
  product:
    cache:
      product-ttl: 3600
      category-ttl: 7200
    search:
      default-page-size: 20
      max-page-size: 100
  tenants:
    - id: tenant_001
      type: SCHEMA
      schema: product_tenant_001
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5433}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}
    - id: tenant_002
      type: SCHEMA
      schema: product_tenant_002
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5433}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}

spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}
    username: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    pool:
      max-size: 10
      initial-size: 2
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}
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

logging:
  level:
    com.iemodo.product: INFO
    org.springframework.data.r2dbc: WARN
EOF

# 创建 inventory-service.yaml
cat > $TEMP_DIR/inventory-service.yaml << 'EOF'
# Inventory Service 配置

iemodo:
  inventory:
    cache:
      stock-ttl: 86400
    allocation:
      max-distance-km: 5000
      default-preference: BALANCED
  tenants:
    - id: tenant_001
      type: SCHEMA
      schema: inventory_tenant_001
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5433}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}
    - id: tenant_002
      type: SCHEMA
      schema: inventory_tenant_002
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5433}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}

spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}
    username: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    pool:
      max-size: 10
      initial-size: 2
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}
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

logging:
  level:
    com.iemodo.inventory: INFO
    org.springframework.data.r2dbc: WARN
EOF

# 创建 file-service.yaml
cat > $TEMP_DIR/file-service.yaml << 'EOF'
# File Service 配置

minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  url-expiry: 3600

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

logging:
  level:
    com.iemodo.file: INFO
    io.minio: WARN
EOF

# 创建 tenant-management.yaml
cat > $TEMP_DIR/tenant-management.yaml << 'EOF'
# Tenant Management Service 配置

iemodo:
  tenants:
    - id: platform
      type: SCHEMA
      schema: tenant_meta
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5433}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}

spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}?schema=tenant_meta
    username: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}?currentSchema=tenant_meta
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

logging:
  level:
    com.iemodo.tenant: INFO
    org.springframework.data.r2dbc: WARN
EOF

# 创建 api-gateway.yaml
cat > $TEMP_DIR/api-gateway.yaml << 'EOF'
# API Gateway 配置

iemodo:
  gateway:
    jwt:
      public-key-path: ${JWT_PUBLIC_KEY_PATH:classpath:jwt/public.pem}
    whitelist:
      - POST:/uc/api/v1/auth/register
      - POST:/uc/api/v1/auth/login
      - POST:/uc/api/v1/auth/refresh
      - GET:/uc/api/v1/auth/oauth2/**
      - POST:/pay/api/v1/webhooks/stripe
      - GET:/actuator/**
      - GET:/health

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
        - id: product-service-route
          uri: lb://product-service
          predicates:
            - Path=/pc/**
        - id: inventory-service-route
          uri: lb://inventory-service
          predicates:
            - Path=/inv/**
        - id: payment-service-route
          uri: lb://payment-service
          predicates:
            - Path=/pay/**
        - id: file-service-route
          uri: lb://file-service
          predicates:
            - Path=/fs/**
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
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}?schema=gateway_config
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
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}?currentSchema=gateway_config
    user: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    schemas: gateway_config
    locations: classpath:db/migration
    baseline-on-migrate: true

logging:
  level:
    com.iemodo: DEBUG
    org.springframework.cloud.gateway: INFO
    com.alibaba.cloud.sentinel: INFO
EOF

# 创建 user-service.yaml
cat > $TEMP_DIR/user-service.yaml << 'EOF'
# User Service 配置

google:
  oauth2:
    client-id: ${GOOGLE_CLIENT_ID:1096869572094-k9d2oqrj4h3i07728mmo78cu4henhj69.apps.googleusercontent.com}
    client-secret: ${GOOGLE_CLIENT_SECRET:replace-with-real-secret}

iemodo:
  user:
    jwt:
      private-key-path: ${JWT_PRIVATE_KEY_PATH:classpath:jwt/private.pem}
      public-key-path: ${JWT_PUBLIC_KEY_PATH:classpath:jwt/public.pem}
      access-token-ttl-minutes: 60
      refresh-token-ttl-days: 30
      issuer: iemodo-user-service
  tenants:
    - id: tenant_001
      type: SCHEMA
      schema: schema_tenant_001
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5433}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}
    - id: tenant_002
      type: SCHEMA
      schema: schema_tenant_002
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5433}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}

spring:
  flyway:
    enabled: true
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}
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

logging:
  level:
    com.iemodo: DEBUG
    org.springframework.security: INFO
    org.flywaydb: INFO
EOF

# 创建 order-service.yaml
cat > $TEMP_DIR/order-service.yaml << 'EOF'
# Order Service 配置

iemodo:
  tenants:
    - id: tenant_001
      type: SCHEMA
      schema: schema_tenant_001
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5433}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}
    - id: tenant_002
      type: SCHEMA
      schema: schema_tenant_002
      host: ${DB_HOST:localhost}
      port: ${DB_PORT:5433}
      database: ${DB_NAME:iemodo}
      username: ${DB_USER:iemodo}
      password: ${DB_PASSWORD:iemodo123}

spring:
  flyway:
    enabled: true
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}
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

logging:
  level:
    com.iemodo: DEBUG
    org.flywaydb: INFO
EOF

# 导入配置的函数
import_config() {
    local data_id=$1
    local file=$2
    
    echo "导入配置: $data_id ..."
    
    # URL encode the content
    local content=$(cat "$file" | python3 -c "import sys, urllib.parse; print(urllib.parse.quote(sys.stdin.read()))" 2>/dev/null || cat "$file")
    
    # 如果 python3 不可用，直接使用文件内容
    if [ -z "$content" ]; then
        content=$(cat "$file")
    fi
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$NACOS_URL/nacos/v1/cs/configs" \
        -d "dataId=$data_id" \
        -d "group=$GROUP" \
        -d "content=$content" \
        -d "type=yaml" \
        -u "$NACOS_USER:$NACOS_PASS" 2>/dev/null || echo "000")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "200" ] && [ "$body" = "true" ]; then
        echo "  ✓ $data_id 导入成功"
    else
        echo "  ✗ $data_id 导入失败 (HTTP $http_code: $body)"
    fi
}

# 检查依赖
echo "检查依赖..."
if ! command -v curl &> /dev/null; then
    echo "错误: 需要安装 curl"
    exit 1
fi

# 检查 Nacos 是否可访问
echo "检查 Nacos 连接..."
if ! curl -s -o /dev/null -w "%{http_code}" "$NACOS_URL/nacos" | grep -q "200\|302"; then
    echo "警告: 无法连接到 Nacos ($NACOS_URL)"
    echo "请确保 Nacos 正在运行"
    echo ""
    read -p "是否继续? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""
echo "开始导入配置..."
echo "======================================"

# 导入所有配置
import_config "payment-service.yaml" "$TEMP_DIR/payment-service.yaml"
import_config "product-service.yaml" "$TEMP_DIR/product-service.yaml"
import_config "inventory-service.yaml" "$TEMP_DIR/inventory-service.yaml"
import_config "file-service.yaml" "$TEMP_DIR/file-service.yaml"
import_config "tenant-management.yaml" "$TEMP_DIR/tenant-management.yaml"
import_config "api-gateway.yaml" "$TEMP_DIR/api-gateway.yaml"
import_config "user-service.yaml" "$TEMP_DIR/user-service.yaml"
import_config "order-service.yaml" "$TEMP_DIR/order-service.yaml"

echo "======================================"
echo "配置导入完成!"
echo ""
echo "已导入的 Data ID:"
echo "  - payment-service.yaml"
echo "  - product-service.yaml"
echo "  - inventory-service.yaml"
echo "  - file-service.yaml"
echo "  - tenant-management.yaml"
echo "  - api-gateway.yaml"
echo "  - user-service.yaml"
echo "  - order-service.yaml"
echo ""
echo "Group: $GROUP"
echo ""
echo "生产环境注意事项:"
echo "  1. 修改 Stripe 密钥 (payment-service.yaml)"
echo "  2. 修改数据库密码 (所有服务)"
echo "  3. 修改 Redis 密码 (所有服务)"
echo "  4. 修改 MinIO 密钥 (file-service.yaml)"
echo "  5. 配置 JWT 密钥 (user-service.yaml)"

# Create pricing-service.yaml
cat > $TEMP_DIR/pricing-service.yaml << 'EOF'
# Pricing Service 配置

# 汇率服务配置
exchange:
  fixer:
    api-key: ${FIXER_API_KEY:}
    base-url: ${FIXER_BASE_URL:https://api.fixer.io}
  base-currency: USD
  cache:
    l1-ttl-minutes: 10
    l2-ttl-hours: 1

# Spring 配置
spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}
    username: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    pool:
      max-size: 10
      initial-size: 2
  flyway:
    enabled: true
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}
    user: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    schemas: pricing
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
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
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
    com.iemodo.pricing: DEBUG
    org.springframework.data.r2dbc: WARN
EOF

import_config "pricing-service.yaml" "$TEMP_DIR/pricing-service.yaml"

# Create tax-service.yaml
cat > $TEMP_DIR/tax-service.yaml << 'EOF'
# Tax Service 配置

spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}
    username: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    pool:
      max-size: 10
      initial-size: 2
  flyway:
    enabled: true
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}
    user: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    schemas: tax
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
    com.iemodo.tax: DEBUG
    org.springframework.data.r2dbc: WARN
EOF

import_config "tax-service.yaml" "$TEMP_DIR/tax-service.yaml"

# Create map-service.yaml
cat > $TEMP_DIR/map-service.yaml << 'EOF'
# Map Service 配置

map:
  google:
    api-key: ${GOOGLE_MAPS_API_KEY:}
    enabled: true
  baidu:
    api-key: ${BAIDU_MAPS_API_KEY:}
    enabled: true
  cache:
    geocode-ttl-hours: 24
    route-ttl-hours: 1

spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:redis123}
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

logging:
  level:
    com.iemodo.map: DEBUG
EOF

import_config "map-service.yaml" "$TEMP_DIR/map-service.yaml"

# Create marketing-service.yaml
cat > $TEMP_DIR/marketing-service.yaml << 'EOF'
# Marketing Service 配置

spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}
    username: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    pool:
      max-size: 10
      initial-size: 2
  flyway:
    enabled: true
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:iemodo}
    user: ${DB_USER:iemodo}
    password: ${DB_PASSWORD:iemodo123}
    schemas: marketing_tenant_001,marketing_tenant_002
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

logging:
  level:
    com.iemodo.marketing: DEBUG
    org.springframework.data.r2dbc: WARN
EOF

import_config "marketing-service.yaml" "$TEMP_DIR/marketing-service.yaml"
