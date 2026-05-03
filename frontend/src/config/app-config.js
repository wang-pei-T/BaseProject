// 说明：
// import.meta.env 是 Vite 提供的“构建时环境变量”入口。
// 例如写了 VITE_API_BASE_URL=http://xxx 后，这里就会读到该值。
// 若环境变量没配置，则使用 || 后面的默认值。
const appConfig = {
  // 浏览器页签标题
  pageTitle: import.meta.env.VITE_APP_PAGE_TITLE || "租赁物管理系统",
  branding: {
    // 左上角 Logo 图片地址（可配成本地 /logo.png 或完整 URL）
    logoUrl: import.meta.env.VITE_APP_LOGO_URL || "/favicon.svg",
    // Logo 的 alt 文本
    logoAlt: import.meta.env.VITE_APP_LOGO_ALT || "logo",
    // 顶栏左侧 Logo 旁边文字
    sidebarTitle: import.meta.env.VITE_APP_SIDEBAR_TITLE || "租赁物管理系统",
  },
  apiBaseUrl:
    typeof import.meta.env.VITE_API_BASE_URL === "string"
      ? import.meta.env.VITE_API_BASE_URL
      : "http://localhost:8080",
  // 布局尺寸配置（预留）
  layout: {
    maxWidth: 1440,
    minWidth: 1080,
    sidebarWidth: "15%",
    sidebarMaxWidth: 265,
    sidebarMinWidth: 180,
    headerHeight: 48,
    breadcrumbHeight: 40,
  },
};

export default appConfig;

