# System 包结构重构 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行。步骤使用 `- [ ]` 勾选跟踪。

**Goal:** 将 `com.baseproject.modules.*` 全部迁入「技术分层 + `system` 域」包结构；`config` / `security` / `common` 根包**不动**；健康检查迁入 `controller.system.common`。

**Architecture:** 采用 `controller.system.*`、`service.system.*`、`domain.system.*`（实体）、`mapper.system.*`（MyBatis Mapper 接口）四层与功能子包对齐；平台能力放在 `*.system.platform.*` 子树下；未来 `biz` 与 `system` 并列（本次不创建 `biz` 空包）。依赖方向保持：Controller → Service → Mapper，`config`/`security` 继续注入各 Service。

**Tech Stack:** Java 8、Spring Boot 2.7、MyBatis-Plus、`@MapperScan`、现有动态数据源与拦截器。

---

## 1. 目标包与源映射总表

**保持不变（本次不移动类文件，仅可能被全局 import 替换引用）：**

- `com.baseproject.BaseProjectApplication`
- `com.baseproject.config.**`
- `com.baseproject.security.**`（含 `DefaultCredentials` 若已在 `security` 下）
- `com.baseproject.common.**`
- `com.baseproject.modules` 删除前需确认无残留引用

**`HealthController` 迁移：**

| 源 | 目标 |
|----|------|
| `com.baseproject.controller.HealthController` | `com.baseproject.controller.system.common.HealthController` |

**租户与通用系统（原 `modules` 下非 platform 前缀）：**

| 功能域 | Controller | Service | Entity / Domain | Mapper |
|--------|------------|---------|-----------------|--------|
| auth | `controller.system.auth` | `service.system.auth` | `domain.system.auth`（`AuthSession`） | — |
| auth DTO | — | `service.system.auth.dto`（`LoginRequest`, `LoginResponse`） | — | — |
| user | `controller.system.user` | `service.system.user` | `domain.system.user`（`SysUser`, `SysUserRole`） | `mapper.system.user` |
| org | `controller.system.org` | `service.system.org` | `domain.system.org`（`SysOrg`） | `mapper.system.org` |
| tenant（表 sys_tenant） | — | — | `domain.system.tenant`（`SysTenant`） | `mapper.system.tenant` |
| role | `controller.system.role` | `service.system.role` | `domain.system.role`（`SysRole`, `SysRolePermission`） | `mapper.system.role` |
| permission | `controller.system.permission` | `service.system.permission` | `domain.system.permission`（`SysPermission`） | `mapper.system.permission` |
| menu | `controller.system.menu` | `service.system.menu` | `domain.system.menu`（`SysMenu`） | `mapper.system.menu` |
| action | `controller.system.action` | `service.system.action` | `domain.system.action`（`SysAction`） | `mapper.system.action` |
| dict | `controller.system.dict` | `service.system.dict` | `domain.system.dict`（`SysDictType`, `SysDictItem`） | `mapper.system.dict` |
| config | `controller.system.config` | `service.system.config` | `domain.system.config`（`SysConfig`） | `mapper.system.config` |
| file | `controller.system.file` | `service.system.file` | `domain.system.file`（`SysFileMeta`） | `mapper.system.file` |
| attachment | `controller.system.attachment` | `service.system.attachment` | `domain.system.attachment`（`SysAttachment`） | `mapper.system.attachment` |
| message | `controller.system.message` | `service.system.message` | `domain.system.message`（`SysMessage`） | `mapper.system.message` |
| tenant audit | `controller.system.tenantaudit` | `service.system.tenantaudit` | `domain.system.tenantaudit`（`SysTenantAudit`） | `mapper.system.tenantaudit` |
| tenant log | `controller.system.tenantlog` | `service.system.tenantlog` | `domain.system.tenantlog`（`SysTenantLog`） | `mapper.system.tenantlog` |
| profile | `controller.system.profile`（`MeController`） | `service.system.profile` | — | `mapper.system.profile`（通知偏好 Mapper） |
| announcement | `controller.system.announcement` | `service.system.announcement` | `domain.system.platform` 或单独 `domain.system.announcement`（`SysPlatformAnnouncement` 属平台表，建议放 `domain.system.platform`） | `mapper.system.platform` |

**平台子域（原 `modules/platform/*`）：**

| 功能域 | Controller | Service | Entity | Mapper |
|--------|--------------|---------|--------|--------|
| platform tenant | `controller.system.platform.tenant` | `service.system.platform.tenant` | `SysTenant` 已在 tenant 域则 Service 引用 `domain.system.tenant` | 同左 |
| platform account | `controller.system.platform.account` | `service.system.platform.account` | `domain.system.platform`（`SysPlatformAccount`, `SysPlatformAccountRole`） | `mapper.system.platform` |
| platform role | `controller.system.platform.role` | `service.system.platform.role` | `domain.system.platform`（`SysPlatformRole`, `SysPlatformRolePerm`） | `mapper.system.platform` |
| platform audit | `controller.system.platform.audit` | `service.system.platform.audit` | `SysPlatformAudit` | `mapper.system.platform` |
| platform assist | `controller.system.platform.assist` | `service.system.platform.assist` | — | — |
| platform permission list | `controller.system.platform.permission` | — 或合并到 audit 同包 | — | — |

**遗留 POJO（若仍存在）：** `TenantOrg`, `TenantUser` → 迁入 `domain.system.org` / `domain.system.user` 或标记删除；以编译引用为准。

---

## 2. 全局机械替换规则（每批迁移后执行）

在 `backend/src/main/java` 内对**已迁移包**做 import 更新（可用 IDE Refactor Move + Optimize Imports，或 `rg`/`sed` 谨慎替换）：

1. `com.baseproject.modules.auth.*` → `com.baseproject.service.system.auth.*` / `controller.system.auth.*` / `domain.system.auth.*`（按类职责选子包）。
2. 其余 `com.baseproject.modules.<x>.*` → 对应 `controller|service|domain|mapper.system.<x>.*`。
3. `com.baseproject.modules.platform` → `...system.platform...` 各子包。

**`BaseProjectApplication` 修改示例：**

```java
@MapperScan({
    "com.baseproject.mapper.system"
})
```

若希望 Mapper 与实体分散扫描，可写多个 base package；**至少**覆盖所有 `*Mapper` 接口新包路径。

**`AuthInterceptor`（`security` 包）**：若存在硬编码 `import com.baseproject.modules...`，改为新 `controller`/`service` 路径。

---

## 3. 分任务清单（建议每任务独立 commit）

### Task 0：目录与健康检查

**Files:**

- Move: `backend/src/main/java/com/baseproject/controller/HealthController.java` → `backend/src/main/java/com/baseproject/controller/system/common/HealthController.java`
- Modify: `HealthController.java` 第一行 `package com.baseproject.controller.system.common;`

- [ ] **Step 1:** 创建目录 `controller/system/common/`，移动文件并改 `package`。
- [ ] **Step 2:** 全仓库 `rg "HealthController"` 确认无旧包引用。
- [ ] **Step 3:** `mvn -f backend/pom.xml -q -DskipTests package`，预期 BUILD SUCCESS。
- [ ] **Step 4:** `git add` + `git commit -m "refactor: move HealthController under controller.system.common"`

---

### Task 1：基础设施 — `MapperScan` 与占位

**Files:**

- Modify: `backend/src/main/java/com/baseproject/BaseProjectApplication.java`（`@MapperScan` 暂可扩为双包：`com.baseproject.modules`、`com.baseproject.mapper.system` 并存，迁移过程中兼容；全部迁完后再删 `modules`）

- [ ] **Step 1:** 将 `@MapperScan` 改为同时扫描 `com.baseproject.mapper.system` 与 `com.baseproject.modules`（过渡期）。
- [ ] **Step 2:** `mvn -q -DskipTests package` SUCCESS。
- [ ] **Step 3:** Commit：`refactor: widen MapperScan for system mapper migration`

---

### Task 2：Auth 竖切迁移

**Files（移动并重命名包，类名不变）：**

- `modules/auth/controller/AuthController.java` → `controller/system/auth/AuthController.java`
- `modules/auth/service/AuthService.java` → `service/system/auth/AuthService.java`
- `modules/auth/dto/*` → `service/system/auth/dto/*`
- `modules/auth/domain/AuthSession.java` → `domain/system/auth/AuthSession.java`

- [ ] **Step 1:** 物理移动上述文件到目标路径。
- [ ] **Step 2:** 每文件修改 `package` 声明；全项目替换对旧包的 import。
- [ ] **Step 3:** `mvn -q -DskipTests package` SUCCESS。
- [ ] **Step 4:** Commit：`refactor(system): relocate auth module`

---

### Task 3：Tenant + Org + User（含 UserRole、SysTenant）

**Files：** 对应 `controller|service|domain|mapper` 下 `org`, `user`, `tenant` 子包；`UserService` 与 `OrgService` 循环依赖保持不变，仅包名变。

- [ ] **Step 1:** 迁移 `tenant`（entity+mapper）、`org`（四层）、`user`（四层含 `SysUserRole`）。
- [ ] **Step 2:** 更新 `RoleService`、`AuthService`、`PlatformTenantService` 等所有引用。
- [ ] **Step 3:** `mvn -q -DskipTests package` SUCCESS。
- [ ] **Step 4:** Commit：`refactor(system): relocate tenant org user`

---

### Task 4：RBAC — Role + Permission

- [ ] 迁移 `role`、`permission` 全部类；更新 `UserService`、`PermissionService`、`PlatformAssistService` 等 import。
- [ ] `mvn -q -DskipTests package` + Commit：`refactor(system): relocate role and permission`

---

### Task 5：Menu + Action + Dict + Config

- [ ] 迁移四个域；更新相互引用与 `ConfigController` 根路径（类上 `@RequestMapping` 勿改 URL）。
- [ ] `mvn -q -DskipTests package` + Commit：`refactor(system): relocate menu action dict config`

---

### Task 6：File + Attachment + Message

- [ ] 迁移；`FileService`/`AttachmentService` 保持依赖关系。
- [ ] `mvn -q -DskipTests package` + Commit：`refactor(system): relocate file attachment message`

---

### Task 7：TenantAudit + TenantLog + Profile

- [ ] 迁移 `audit`、`log`、`profile`（含 `SysUserNotificationPrefMapper`）；更新 `ProfileService`、拦截器若引用 Controller 全限定名（一般无）。
- [ ] `mvn -q -DskipTests package` + Commit：`refactor(system): relocate audit log profile`

---

### Task 8：Announcement + Platform 全 subtree

**Files：** `announcement`、`platform/tenant|account|role|audit|assist|permission` 的 controller/service；`platform/entity`、`platform/mapper` → `domain.system.platform` + `mapper.system.platform`（或拆分子包视文件数量而定）。

- [ ] **Step 1:** 先迁 `mapper.system.platform` + `domain.system.platform` 下所有 `SysPlatform*` 与 `SysPlatformAnnouncement`。
- [ ] **Step 2:** 迁各 `Platform*Service` / `Platform*Controller`。
- [ ] **Step 3:** `mvn -q -DskipTests package` SUCCESS。
- [ ] **Step 4:** Commit：`refactor(system): relocate announcement and platform`

---

### Task 9：清理 `modules`、收窄 `MapperScan`

- [ ] `rg "com.baseproject.modules"` 在 `backend/src/main/java` 应为 **0 命中**。
- [ ] 删除空目录 `backend/src/main/java/com/baseproject/modules`（及子目录）。
- [ ] `BaseProjectApplication`：`@MapperScan("com.baseproject.mapper.system")` 单包（若 profile mapper 在 `mapper.system.profile` 仍被包含）。
- [ ] `@SpringBootApplication` 默认扫描 `com.baseproject`，无需改 basePackage。
- [ ] `mvn -q -DskipTests package` + Commit：`refactor: remove legacy modules package`

---

### Task 10：验收

- [ ] 全量：`mvn -f backend/pom.xml -q -DskipTests package`
- [ ] 若有 Spring 测试：`mvn -f backend/pom.xml test`
- [ ] 手动启动应用，抽样请求：`/actuator` 或健康接口、`/tenant/users`、`/platform/tenants`（以项目实际路径为准）

---

## 4. 自审（Spec coverage）

| 需求 | 对应任务 |
|------|----------|
| controller 下 system 子文件夹 | Task 0–8 |
| 各层均有 system | 总表 + Task 2–8 |
| config/security 不动 | 明确保留 + Task 9 不触碰 |
| biz 未涉及 | 不创建 `biz` 包；注释中可写后续并列方案 |
| Health 放入 system.common | Task 0 |

**Placeholder 扫描：** 本计划无 TBD 实现细节；具体类名以仓库当前文件为准，迁移时以 `glob` 为准绳。

---

## 5. 执行交接

**计划已保存到：** `doc/superpowers/plans/2026-04-28-system-package-restructure.md`

**两种执行方式：**

1. **Subagent-Driven（推荐）** — 每个 Task 派生子代理执行，任务间人工或代理复核。需使用 skill：`superpowers:subagent-driven-development`。
2. **Inline Execution** — 本会话内按 Task 顺序执行，配合 skill：`superpowers:executing-plans` 做检查点。

**请选择 1 或 2；若不指定，默认按 2 在本会话逐项执行（你回复「执行」或「1/2」即可）。**
