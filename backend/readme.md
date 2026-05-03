# BaseProject 后端（Spring Boot）

租户与平台管理 REST API，与前端 SPA 分离部署。总览、库初始化与默认账号见仓库根 [README.md](../README.md)；前端代理与环境变量见 [frontend/README.md](../frontend/README.md)。

---

## 技术栈与版本

| 类别 | 技术 | 说明 |
|------|------|------|
| 运行时 | Java **8** | 与 `pom.xml` 中 `java.version` 一致 |
| 框架 | Spring Boot **2.7.18** | parent BOM |
| Web | spring-boot-starter-web | REST |
| 校验 | spring-boot-starter-validation | |
| 密码 | spring-security-crypto | BCrypt 等 |
| ORM | MyBatis-Plus **3.5.7** | |
| 数据源 | dynamic-datasource-spring-boot-starter **3.6.1** | 多数据源 `core` / `biz` |
| 缓存 | spring-boot-starter-data-redis | 健康检查等使用 Redis |
| 对象存储 | MinIO Java SDK **8.5.17** | 上传、预签名 URL；桶不存在时可自动创建 |
| 文档 | springdoc-openapi-ui **1.7.0** | Swagger UI |
| 数据库 | MySQL（mysql-connector-j） | JDBC URL 见各 profile |

完整依赖见 [pom.xml](pom.xml)。

---

## 工程结构（包约定）

根包：`com.baseproject`

| 包 / 目录 | 职责 |
|-----------|------|
| `controller` | HTTP 入参、出参；按 `system` 子域划分（auth、tenant、platform 等） |
| `service` | 业务逻辑；大量类标注 `@DS("core")` 访问主库 |
| `domain` | 实体与领域对象 |
| `mapper` | MyBatis-Plus Mapper |
| `config` | Spring 配置（WebMvc、全局异常、请求 ID、MinIO 等） |
| `security` | `AuthInterceptor`、`AuthContext`、平台权限拦截等 |
| `job` | 定时任务（如数据留存清理） |
| `util` | 工具类（如脱敏） |
| `common` | 公共属性类（如 MinIO 配置绑定） |

主启动类：[BaseProjectApplication.java](src/main/java/com/baseproject/BaseProjectApplication.java)

- `@MapperScan("com.baseproject.mapper.system")`
- `@EnableScheduling`：启用定时任务
- `@ConfigurationPropertiesScan`：扫描如 `OpsLogsProperties` 等配置类

---

## 配置文件说明

| 文件 | 用途 |
|------|------|
| [src/main/resources/application.yml](src/main/resources/application.yml) | 公共项：端口 **8080**、默认 `spring.profiles.active=dev`、multipart 上限、`baseproject.*`（安全默认密码占位、子域租户开关、`ops-logs`、机构删除策略）、MyBatis-Plus、springdoc、日志文件路径 |
| [src/main/resources/application-dev.yml](src/main/resources/application-dev.yml) | 本地开发：双数据源 JDBC、Redis、MinIO；可开启子域租户与 `hosts` 说明 |
| [src/main/resources/application-prod.yml](src/main/resources/application-prod.yml) | 生产：数据源与 Redis/MinIO 使用**环境变量**占位，勿在仓库填真实密钥 |
| `application-uat.yml` / `application-test.yml` | 其它环境（按需使用） |

### `baseproject` 常用配置项（application.yml）

- **`baseproject.security.default-password`**：重置密码等场景的默认明文策略说明（生产务必收紧或改为强制随机）。
- **`baseproject.auth.tenant-from-host`**：从 Host 解析租户；生产需网关传 `Host` / `X-Forwarded-Host`（注释见 yml）。
- **`baseproject.ops-logs`**：`mode`（`none` | `external` | `loki`）、`external-url`（可含 `{{tenantId}}` 由接口替换）、`loki-base-url`。
- **`baseproject.org.delete`**：删除机构前是否要求无子机构、无用户。

---

## 数据源与 `@DS`

动态数据源定义在 `spring.datasource.dynamic` 下，主数据源名为 **`core`**，另预留 **`biz`**（与 `application-dev.yml` / `application-prod.yml` 中配置块一致）。

当前业务 Service 普遍使用 **`@DS("core")`**；代码库中未见 `@DS("biz")"`。若后续拆分读写或业务库，可在 Service 上切换/组合数据源注解。

租户、操作者等上下文见 **`AuthContext`**（由 `AuthInterceptor` 在验票后注入）。

---

## Redis 与 MinIO

- **Redis**：Spring Data Redis 自动配置；[HealthController](src/main/java/com/baseproject/controller/system/common/HealthController.java) 对 Redis 执行 `PING` 作为健康检查的一部分。开发环境需启动 Redis，否则相关 Bean/健康检查可能失败（视启动严格程度而定）。
- **MinIO**：配置项前缀 `minio.*`（endpoint、access-key、secret-key、bucket）。上传逻辑见 [FileService](src/main/java/com/baseproject/service/system/file/FileService.java)：若桶不存在会尝试 `makeBucket`。

---

## 运行与打包

### 开发直接运行

```bash
mvn spring-boot:run
```

工作目录为 `backend/`，默认使用 `application.yml` + `application-dev.yml`（`spring.profiles.active=dev`）。

### 打包

```bash
mvn -DskipTests package
```

可执行 JAR：`target/backend-0.0.1-SNAPSHOT.jar`（`artifactId` + `version` 见 [pom.xml](pom.xml)）。

### 运行示例（生产 profile）

```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

生产环境变量示例见 [application-prod.yml](src/main/resources/application-prod.yml)：`CORE_DB_URL`、`REDIS_HOST`、`MINIO_ENDPOINT` 等。

### 日志

默认写文件：`logs/baseproject-backend.log`（见 `application.yml` 的 `logging.file`）。

---

## HTTP 与鉴权

### 拦截路径

[WebMvcConfig](src/main/java/com/baseproject/config/WebMvcConfig.java) 注册拦截器，匹配：

- `/api/**`、`/auth/**`、`/tenant/**`、`/platform/**`

排除：`/api/ping`（匿名探测）。

### 匿名访问

[AuthInterceptor](src/main/java/com/baseproject/security/AuthInterceptor.java)：`POST /auth/login`、`GET /auth/tenant-login-options` 等无需 Bearer Token（以代码为准）。

### CORS

`WebMvcConfig#addCorsMappings` 对 `/**` 允许跨域，便于前端直连后端开发；生产建议收敛为固定源 + 网关统一策略。

### Swagger / OpenAPI

- UI：`/swagger-ui.html`
- JSON：`/v3/api-docs`

与 [application.yml](src/main/resources/application.yml) 中 `springdoc` 节点一致。

---

## 定时任务与配置键

### 审计 / 租户日志留存

类：[TenantDataRetentionJob](src/main/java/com/baseproject/job/TenantDataRetentionJob.java)

- 默认 **每日 03:00**（cron `0 0 3 * * ?`）
- 配置键（`sys_config` / 默认值见 [ConfigDefaults](src/main/java/com/baseproject/service/system/config/ConfigDefaults.java)）：
  - `audit.retention.days`（默认 365）
  - `tenant.log.retention.days`（默认 90）
- 通过 [EffectiveConfigService](src/main/java/com/baseproject/service/system/config/EffectiveConfigService.java) 读取租户 `0` 的平台层配置（与任务实现一致）。

### 运行日志（运维外链）

- 配置类：[OpsLogsProperties](src/main/java/com/baseproject/config/OpsLogsProperties.java)（前缀 `baseproject.ops-logs`）
- 接口：`GET /tenant/ops-logs/config`（[TenantOpsLogController](src/main/java/com/baseproject/controller/system/opslog/TenantOpsLogController.java)）
- 需权限码：**`tenant.ops_log.read`**（当前用户租户权限集合需包含该码）
- `external-url` 中可包含字面量 `{{tenantId}}`，接口返回前替换为当前登录用户租户 ID

说明：此能力用于嵌入 Grafana / Loki 等**集中式**运维日志，不是读取本机 JVM 控制台日志文件。

---

## 数据库脚本

与仓库根 [README.md](../README.md) 中 **SQL** 章节一致：`sql/init_schema.sql` → `sql/init_data.sql`；可选 `sql/demo_data.sql`。

---

## 安全注意事项

1. **切勿**将生产数据库密码、MinIO 密钥、JWT 密钥等写入 Git 跟踪的配置文件；使用环境变量或密钥管理系统。
2. 修改默认管理员密码；限制 Swagger 在生产环境的暴露范围（IP 白名单或关闭）。
3. `baseproject.security.default-password` 仅作开发/产品策略参考，上线前需评估。

---

## 常见问题（FAQ）

| 现象 | 排查方向 |
|------|-----------|
| 无法连接数据库 | JDBC URL、账号、库名 `baseproject`、MySQL 时区参数 `serverTimezone` |
| Redis 连接失败 | `spring.redis.*` 与进程是否监听 |
| 上传失败 / MinIO 报错 | endpoint 是否可达、access/secret、bucket 名称与权限 |
| 前端时间筛选异常 | 后端审计/日志使用 `Instant.parse`（ISO-8601）；注意 UTC 与本地时间 |
| 跨域已开但仍 401 | 检查请求是否携带 `Authorization: Bearer <token>`，以及路径是否落在拦截器规则内 |

---

## 相关文档

- [../README.md](../README.md) — 仓库总览、初始化顺序、联调步骤  
- [../frontend/README.md](../frontend/README.md) — `VITE_API_BASE_URL`、Vite 代理、子域开发  
