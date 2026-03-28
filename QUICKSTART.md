# iemodo 本地启动快速参考

## 📋 前提条件

确保已安装：
- Docker & Docker Compose
- Java 21 (OpenJDK)
- Maven 3.9+

## 🚀 快速启动

```bash
# 进入项目目录
cd /Users/breanna/Documents/code/iemodo

# 1. 启动基础设施 (PostgreSQL, Redis, Nacos)
./start-local.sh infra

# 2. 编译项目 (首次需要)
mvn clean compile -DskipTests -q

# 3. 启动所有微服务
./start-local.sh start

# 4. 查看状态
./start-local.sh status
```

## 📊 常用命令

| 命令 | 说明 |
|------|------|
| `./start-local.sh infra` | 只启动基础设施 |
| `./start-local.sh full` | 完整启动 (编译+启动) |
| `./start-local.sh start` | 快速启动 (跳过编译) |
| `./start-local.sh stop` | 停止所有服务 |
| `./start-local.sh restart` | 重启所有服务 |
| `./start-local.sh status` | 查看服务状态 |
| `./start-local.sh logs [服务名]` | 查看日志 |

## 🌐 访问地址

| 服务 | URL | 账号/密码 |
|------|-----|----------|
| API Gateway | http://localhost:8080 | - |
| Nacos | http://localhost:8848/nacos | nacos/nacos |
| PostgreSQL | localhost:5433 | iemodo/iemodo |
| Redis | localhost:6379 | redis123 |

## 🔧 服务端口

```
8080 - API Gateway         8081 - User Service
8082 - Product Service     8083 - Order Service
8084 - Inventory Service   8085 - Payment Service
8086 - Pricing Service     8087 - Map Service
8088 - Tax Service         8089 - Marketing Service
8090 - File Service        8091 - Tenant Management
8092 - Fulfillment Service
```

## 📡 API 测试

```bash
# 用户注册
curl -X POST http://localhost:8080/uc/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -H "X-TenantID: tenant_001" \
  -d '{
    "email": "test@example.com",
    "password": "TestPassword123!",
    "displayName": "Test User"
  }'

# 用户登录
curl -X POST http://localhost:8080/uc/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-TenantID: tenant_001" \
  -d '{
    "email": "test@example.com",
    "password": "TestPassword123!"
  }'

# 健康检查
curl http://localhost:8080/actuator/health
```

## 🐛 故障排查

### 端口被占用
```bash
# 查看端口占用
lsof -i :8080

# 结束进程
kill -9 <PID>
```

### Docker 容器问题
```bash
# 查看容器状态
docker ps

# 重启容器
docker restart iemodo-postgres iemodo-redis iemodo-nacos

# 删除并重新创建
docker rm -f iemodo-postgres iemodo-redis iemodo-nacos
./start-local.sh infra
```

### 查看日志
```bash
# 查看所有服务日志
tail -f logs/*.log

# 查看特定服务日志
./start-local.sh logs user-service
tail -f logs/user-service.log
```

### 编译问题
```bash
# 清理并重新编译
mvn clean compile -DskipTests

# 跳过测试编译
mvn clean install -DskipTests
```

## 📁 目录结构

```
iemodo/
├── start-local.sh          # 启动脚本
├── QUICKSTART.md           # 本文件
├── api-gateway/            # API 网关
├── user-service/           # 用户服务
├── product-service/        # 商品服务
├── order-service/          # 订单服务
├── inventory-service/      # 库存服务
├── payment-service/        # 支付服务
├── pricing-service/        # 定价服务
├── tax-service/            # 税务服务
├── map-service/            # 地图服务
├── marketing-service/      # 营销服务
├── file-service/           # 文件服务
├── tenant-management/      # 租户管理
├── fulfillment-service/    # 履约服务
└── logs/                   # 日志目录
```

## 💡 提示

1. **首次启动**：运行 `./start-local.sh full` 会自动编译并启动所有服务
2. **开发模式**：修改代码后，可以使用 `./start-local.sh restart` 重启
3. **只改一个服务**：可以直接进入服务目录运行 `mvn spring-boot:run`
4. **内存优化**：如果内存不足，可以分批启动服务

## 🆘 需要帮助

- 查看详细日志：`tail -f logs/<service-name>.log`
- 检查服务健康：`curl http://localhost:<port>/actuator/health`
- Nacos 控制台：http://localhost:8848/nacos
