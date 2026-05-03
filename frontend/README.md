# BaseProject 前端（React + Vite）

单页应用（SPA），与 Spring Boot 后端分离。仓库总览与数据库初始化见根目录 [README.md](../README.md)；后端配置、Swagger、定时任务见 [backend/readme.md](../backend/readme.md)。

---

## 技术栈与版本

主要依赖见 [package.json](package.json)：

| 依赖 | 说明 |
|------|------|
| React **19** / React DOM | UI 运行时 |
| Vite **8** | 开发与构建 |
| Ant Design **6** | 组件库 |
| @ant-design/icons **6** | 图标 |
| react-router-dom **7** | 路由 |
| zustand **5** | 状态（登录态、运行时配置等） |
| axios **1** | HTTP 客户端 |
| dayjs | 日期（审计/日志筛选等） |
| xlsx | 表格导出 |

开发工具：ESLint 10、@vitejs/plugin-react 6（见 `devDependencies`）。

---

## 目录结构（`src/`）

| 路径 | 说明 |
|------|------|
| [src/main.jsx](src/main.jsx) / [src/App.jsx](src/App.jsx) | 入口与壳布局（侧栏、顶栏、路由出口） |
| [src/router/index.jsx](src/router/index.jsx) | 路由表：租户页、平台页、`/login`、`/platform/login` |
| [src/pages/](src/pages/) | 页面组件：用户、机构、角色、菜单、字典、配置、会话、消息、审计、日志、运行日志、平台各模块、登录与个人中心等 |
| [src/api/](src/api/) | 按域划分的接口封装；统一使用 [src/api/http.js](src/api/http.js) |
| [src/components/](src/components/) | 通用 UI（如 `PageShell`、`QueryBar`、`TableCard`） |
| [src/store/](src/store/) | Zustand：`auth`、`runtime-config`、`app-store` 等 |
| [src/config/app-config.js](src/config/app-config.js) | 读取 `import.meta.env.VITE_*`，组装 `apiBaseUrl`、品牌等 |
| [src/theme/](src/theme/) | Ant Design 主题扩展 |

---

## 环境变量（`VITE_*`）

构建时由 Vite 注入，修改后需重新 `npm run dev` / `npm run build`。可在仓库 [`.env`](.env)、`.env.development`、`.env.production` 中维护（本地 `.env` 若未入库，请自建）。

| 变量 | 必填 | 说明 |
|------|------|------|
| `VITE_API_BASE_URL` | 建议配置 | 后端 API 根地址。未设置时回退为 **`http://localhost:8080`**（见 `app-config.js`）。设为 **空字符串 `""`** 时，请求走**相对当前页 origin 的路径**，配合 Vite 开发代理将 `/auth`、`/tenant` 等转发到本机后端（见下文「开发代理」）。 |
| `VITE_APP_PAGE_TITLE` | 否 | 浏览器页签标题，默认「租赁物管理系统」 |
| `VITE_APP_LOGO_URL` | 否 | 顶栏 Logo，默认 `/favicon.svg` |
| `VITE_APP_LOGO_ALT` | 否 | Logo 的 `alt` 文本 |
| `VITE_APP_SIDEBAR_TITLE` | 否 | 顶栏左侧标题文案 |

**与后端地址的关系（摘要）**

- **直连后端**：`VITE_API_BASE_URL=http://localhost:8080`（或网关 URL）。需后端开启 CORS（后端已配置全局 CORS）。
- **开发代理**：`VITE_API_BASE_URL=""`，浏览器请求发往 Vite 开发服务器（如 `http://default.localhost:5173/tenant/...`），由 [vite.config.js](vite.config.js) 代理到 `http://127.0.0.1:8080`，并**保留原始 Host**（`preserveBrowserHost`），便于后端按子域解析租户；详见后端 `application-dev.yml` 顶部注释与根 README「子域租户」小节。

---

## 本地开发

```bash
npm install
npm run dev
```

等价脚本：`npm start`（见 `package.json`）。

- 默认开发端口一般为 **5173**（Vite 默认，以终端输出为准）。
- 确保后端已启动（默认 `http://127.0.0.1:8080`）且 MySQL/Redis/MinIO 等按后端 README 就绪。

### 开发代理（[vite.config.js](vite.config.js)）

匹配路径正则：`^/(auth|tenant|platform|api)` → 目标 `http://127.0.0.1:8080`，`changeOrigin: false`，并转发浏览器 `Host`。

典型用途：子域租户联调（本机 `hosts` 增加 `127.0.0.1 default.localhost` 等，与后端 `baseproject.auth.tenant-from-host` 配置一致）。

---

## 构建、预览与产物

```bash
npm run build
npm run preview
```

- **产物目录**：`dist/`，为静态 HTML/JS/CSS，可部署到任意静态文件服务器或 CDN。
- **生产环境**：构建前设置 `VITE_API_BASE_URL` 为生产网关或后端 HTTPS 根地址；注意 HTTPS 与 Cookie/混合内容策略。
- **Nginx SPA**：`location / { try_files $uri $uri/ /index.html; }`，并将 API 反代到后端或单独域名。

---

## HTTP 客户端与鉴权（[src/api/http.js](src/api/http.js)）

- **baseURL**：来自 `appConfig.apiBaseUrl`（由 `VITE_API_BASE_URL` 决定）。
- **请求头**：若 `localStorage` 中存在 `accessToken`，自动附加 `Authorization: Bearer <token>`。
- **401**：清除 `accessToken`、`tokenExpiresAt`、`authUser`；若 `authUser.principalType === PLATFORM_ACCOUNT` 则跳转 **`/platform/login`**，否则 **`/login`**。
- **403**：浏览器 `alert('无权限访问')`（可按产品改为统一提示组件）。

### 租户权限字段

登录成功后，后端返回的用户对象中，租户用户可包含 **`tenantPermissions`**（权限码字符串数组），用于前端按钮级展示判断（例如运行日志页需 `tenant.ops_log.read`）。平台账号使用 `platformPermissions` 等字段（以后端登录响应为准）。

---

## 代码质量

```bash
npm run lint
```

ESLint 配置：[eslint.config.js](eslint.config.js)。

---

## 常见问题（FAQ）

| 问题 | 说明 |
|------|------|
| 接口 404 | 确认最终请求 URL：axios `baseURL` 与路径拼接后，应对应后端实际前缀（如 `/tenant/...`、`/auth/...`）。开发代理仅匹配上述前缀。 |
| 跨域错误 | 直连后端时检查后端 CORS；使用代理时确认 `VITE_API_BASE_URL=""` 且页面访问的是 Vite dev 的 origin。 |
| 登录后仍 401 | Token 是否写入 `localStorage`；系统时间是否严重偏差导致提前过期。 |
| 构建后白屏 | 检查静态资源部署路径是否需设置 Vite `base`；控制台是否有资源 404。 |
| 子域租户不生效 | 核对 `hosts`、浏览器访问域名、后端 `tenant-from-host` 与前端是否走代理保留 Host。 |

---

## 相关文档

- [../README.md](../README.md) — 全项目快速开始、SQL 顺序、默认账号  
- [../backend/readme.md](../backend/readme.md) — 后端 profile、Swagger、数据源与运维配置  

---

## 附录：`.env` 示例（本地直连）

在 `frontend/.env` 中可配置（示例，按本机修改）：

```env
VITE_API_BASE_URL=http://localhost:8080
```

若采用**开发代理 + 子域租户**，可改为：

```env
VITE_API_BASE_URL=
```

并配合 `hosts` 与后端 `application-dev.yml` 中的子域说明使用。
