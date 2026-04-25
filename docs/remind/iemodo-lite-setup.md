# iemodo-lite 单服务分支改造记录

## 背景

将 16 微服务 + Nacos 架构的 iemodo 项目精简为单 JAR 单体应用，极致节省资源，适合小型化部署。

## 改动内容 (2026-04-25)

### 分支
- `iemodo-lite` 分支，从 `main` 分出

### 新增文件
- `app-boot/` — 单体应用入口模块，依赖所有服务模块
- `app-boot/src/main/java/com/iemodo/IemodoApplication.java` — 唯一 @SpringBootApplication
- `app-boot/src/main/java/com/iemodo/config/JwtWebFilter.java` — WebFilter 版 JWT 校验（代替 Gateway GlobalFilter）
- `app-boot/src/main/java/com/iemodo/config/LiteCorsConfig.java` — CORS 配置
- `app-boot/src/main/java/com/iemodo/config/LiteGatewayProperties.java` — JWT 配置属性
- `app-boot/src/main/java/com/iemodo/config/LiteR2dbcConfig.java` — 统一 R2DBC Repository 扫描
- `app-boot/src/main/resources/application.yml` — 统一配置，无 Nacos
- `Dockerfile` — 单体应用容器化

### 删除/修改
- 所有 16 个服务的 application.yml：移除 Nacos spring.config.import + spring.cloud.nacos 配置
- 6 个服务的 pom.xml：移除 nacos-discovery + nacos-config 依赖
- 16 个服务的 Application.java：移除 @SpringBootApplication 等注解
- 5 个服务的 *R2dbcConfig：移除 @EnableR2dbcRepositories（由 LiteR2dbcConfig 统一管理）
- docker-compose.yml：移除 Nacos 容器
- common 模块：修复 BaseEntity @Builder.Default 警告、SlowQueryProxyListener 接口名错误

### 资源对比
| 资源 | 微服务版 (main) | 单服务版 (iemodo-lite) |
|------|---------------|---------------------|
| Java 进程数 | 16 + Nacos = 17 | 1 |
| 最少内存 | ~5GB+ | ~1.5GB |
| Docker 依赖 | PostgreSQL + Redis + Nacos | PostgreSQL + Redis |

### 验证
- `mvn package -pl app-boot -am -Dmaven.test.skip=true` — BUILD SUCCESS
- JAR 启动正常，40 个 R2DBC Repository 全量加载，多租户 ConnectionFactory 初始化成功
