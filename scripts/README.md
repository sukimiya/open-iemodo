# Nacos 配置管理脚本

## 脚本说明

### 1. import-nacos-configs.sh
一键导入所有服务的 Nacos 配置。

**使用方式:**
```bash
# 默认连接 localhost:8848
./import-nacos-configs.sh

# 指定 Nacos 地址和认证信息
./import-nacos-configs.sh http://localhost:8848 nacos nacos
```

**导入的配置列表:**
- payment-service.yaml (Stripe 支付配置)
- product-service.yaml (商品服务配置)
- inventory-service.yaml (库存服务配置)
- file-service.yaml (MinIO 文件存储配置)
- tenant-management.yaml (租户管理配置)
- api-gateway.yaml (网关路由配置)
- user-service.yaml (用户服务配置)
- order-service.yaml (订单服务配置)

### 2. verify-nacos-configs.sh
验证 Nacos 中是否存在所有必要的配置。

**使用方式:**
```bash
./verify-nacos-configs.sh

# 或指定 Nacos 地址
./verify-nacos-configs.sh http://localhost:8848 nacos nacos
```

## 前置条件

1. Nacos 服务必须正在运行
   ```bash
   docker compose up -d nacos
   ```

2. 安装 curl
   ```bash
   # macOS
   brew install curl
   
   # Ubuntu/Debian
   apt-get install curl
   ```

## 配置说明

所有配置使用 **Group: IEMODO**，格式为 **YAML**。

### 多租户 Schema 命名规则

| 服务 | Tenant 001 Schema | Tenant 002 Schema |
|------|-------------------|-------------------|
| User/Order | `schema_tenant_001` | `schema_tenant_002` |
| Product | `product_tenant_001` | `product_tenant_002` |
| Inventory | `inventory_tenant_001` | `inventory_tenant_002` |
| Payment | `payment_tenant_001` | `payment_tenant_002` |
| Tenant Meta | `tenant_meta` | - |

### 环境变量

配置中使用了以下环境变量（有默认值）:

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| DB_HOST | localhost | PostgreSQL 主机 |
| DB_PORT | 5432 | PostgreSQL 端口 |
| DB_NAME | iemodo | 数据库名 |
| DB_USER | iemodo | 数据库用户 |
| DB_PASSWORD | iemodo123 | 数据库密码 |
| REDIS_HOST | localhost | Redis 主机 |
| REDIS_PORT | 6379 | Redis 端口 |
| REDIS_PASSWORD | redis123 | Redis 密码 |
| NACOS_SERVER_ADDR | localhost:8848 | Nacos 地址 |

## 生产环境配置

导入配置后，需要在 Nacos 控制台修改以下敏感配置:

### 1. Stripe 密钥 (payment-service.yaml)
```yaml
stripe:
  public-key: pk_live_your_real_key
  secret-key: sk_live_your_real_key
  webhook-secret: whsec_your_real_secret
```

### 2. 数据库密码 (所有服务)
修改 `spring.r2dbc.password` 和 `spring.flyway.password`

### 3. Redis 密码 (所有服务)
修改 `spring.data.redis.password`

### 4. MinIO 密钥 (file-service.yaml)
```yaml
minio:
  access-key: your-access-key
  secret-key: your-secret-key
```

### 5. OAuth2 密钥 (user-service.yaml)
```yaml
google:
  oauth2:
    client-id: 1096869572094-k9d2oqrj4h3i07728mmo78cu4henhj69.apps.googleusercontent.com  # 已配置
    client-secret: your-google-client-secret  # 需要配置
```

**注意**: Client ID 已预配置，但 **Client Secret 必须保密**，通过环境变量或 Nacos 控制台设置。

### 6. JWT 密钥
确保 `user-service/src/main/resources/jwt/` 目录下有:
- `private.pem` - RSA 私钥
- `public.pem` - RSA 公钥

生成方式:
```bash
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
```

## 故障排查

### 1. 导入失败
检查 Nacos 是否运行:
```bash
curl http://localhost:8848/nacos/v1/ns/operator/metrics
```

### 2. 服务启动时无法读取配置
- 检查 `spring.config.import` 是否正确
- 检查 Group 是否为 `IEMODO`
- 检查 Data ID 是否与文件名一致

### 3. 配置修改后未生效
- 配置修改后自动刷新需要 `refreshEnabled=true`
- 部分配置（如数据库连接）需要重启服务
