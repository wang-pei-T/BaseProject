import { useEffect, useMemo, useState } from "react";
import { Link, Navigate, Outlet, useLocation, useNavigate } from "react-router-dom";
import {
  Layout,
  Menu,
  Dropdown,
  Button,
  Space,
  Avatar,
  Breadcrumb,
  theme as antdThemeApi,
  ConfigProvider,
  App as AntdAppProvider,
} from "antd";
import axios from "axios";
import {
  HomeOutlined,
  UserOutlined,
  ApartmentOutlined,
  SafetyCertificateOutlined,
  MenuOutlined,
  BookOutlined,
  SettingOutlined,
  LockOutlined,
  FileTextOutlined,
  ProfileOutlined,
  BankOutlined,
  BellOutlined,
  LogoutOutlined,
  CloudOutlined,
  ToolOutlined,
  AuditOutlined,
  FileSearchOutlined,
  MonitorOutlined,
} from "@ant-design/icons";
import appConfig from "./config/app-config";
import { getMenuIcon } from "./config/menu-icons";
import { darkNeutralChromeOverrides } from "./theme/antd-theme";
import useAuthStore from "./store/auth";
import useAppStore from "./store/app-store";
import { queryMyMenus } from "./api/menu";
import useRuntimeConfigStore from "./store/runtime-config";
import { clearAccessTokenExpirySchedule, scheduleAccessTokenExpiry } from "./store/token-expiry";
import { filterPlatformMenuItems } from "./utils/platform-perm";

const { Header, Sider, Content } = Layout;

const PATH_TITLE = {
  "/": "工作台",
  "/users": "用户管理",
  "/orgs": "机构管理",
  "/roles": "角色管理",
  "/menus": "菜单管理",
  "/dicts": "字典管理",
  "/configs": "配置中心",
  "/sessions": "会话管理",
  "/messages": "消息中心",
  "/audits": "审计管理",
  "/logs": "日志管理",
  "/runtime-logs": "运行日志",
  "/profile": "个人中心",
  "/platform/tenants": "租户管理",
  "/platform/accounts": "平台账号",
  "/platform/roles": "平台角色",
  "/platform/audits": "平台审计",
  "/platform/announcements": "平台公告",
  "/platform/configs": "平台配置",
  "/platform/assist": "跨租户协助",
  "/platform/logs": "平台日志",
};

function findKeyPathFromItems(items, targetKey) {
  if (!targetKey || !items?.length) return null;
  function walk(list, chain) {
    for (const item of list) {
      const next = [...chain, item.key];
      if (String(item.key) === String(targetKey)) return next;
      if (item.children?.length) {
        const hit = walk(item.children, next);
        if (hit) return hit;
      }
    }
    return null;
  }
  return walk(items, []);
}

const PATH_GROUP = {
  "/users": "系统管理",
  "/orgs": "系统管理",
  "/roles": "系统管理",
  "/menus": "系统管理",
  "/dicts": "系统管理",
  "/configs": "系统管理",
  "/sessions": "系统管理",
  "/audits": "系统管理",
  "/logs": "系统管理",
  "/runtime-logs": "系统管理",
  "/platform/tenants": "平台运营",
  "/platform/accounts": "平台运营",
  "/platform/roles": "平台运营",
  "/platform/audits": "平台运营",
  "/platform/announcements": "平台运营",
  "/platform/configs": "平台运营",
  "/platform/assist": "平台运营",
  "/platform/logs": "平台运营",
};

function ThemedShell({
  dark,
  branding,
  menuItems,
  selectedKeys,
  menuOpenKeys,
  onMenuOpenChange,
  breadcrumbItems,
  displayName,
  avatarChar,
  userMenuItems,
  pingBackend,
  toggleTheme,
  showMessagesLink,
}) {
  const { token } = antdThemeApi.useToken();
  return (
    <Layout style={{ minHeight: "100vh", background: token.colorBgLayout }}>
      <Header
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          paddingInline: 16,
          background: "#008c8c",
          height: 48,
          lineHeight: "48px",
        }}
      >
        <Link to="/" style={{ display: "flex", alignItems: "center", gap: 10, color: "#fff", textDecoration: "none" }}>
          <img src={branding.logoUrl} alt={branding.logoAlt || "logo"} style={{ height: 28 }} />
          <span style={{ fontWeight: 600, fontSize: 16 }}>{branding.sidebarTitle}</span>
        </Link>
        <Space size="middle">
          {showMessagesLink ? (
            <Link to="/messages" style={{ color: "#fff", fontSize: 18 }}>
              <BellOutlined />
            </Link>
          ) : null}
          <Button type="text" icon={<CloudOutlined />} onClick={pingBackend} style={{ color: "#fff" }} />
          <Button type="text" onClick={toggleTheme} style={{ color: "#fff" }}>
            {dark ? "浅色" : "深色"}
          </Button>
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <Space style={{ cursor: "pointer", color: "#fff" }}>
              <Avatar style={{ background: "rgba(255,255,255,0.25)" }}>{avatarChar}</Avatar>
              <span>{displayName}</span>
            </Space>
          </Dropdown>
        </Space>
      </Header>
      <Layout style={{ background: token.colorBgLayout }}>
        <Sider width={220} theme={dark ? "dark" : "light"} style={{ borderRight: `1px solid ${token.colorBorderSecondary}` }}>
          <Menu
            mode="inline"
            selectedKeys={selectedKeys}
            openKeys={menuOpenKeys}
            onOpenChange={onMenuOpenChange}
            style={{ borderInlineEnd: 0, height: "100%" }}
            items={menuItems}
          />
        </Sider>
        <Layout style={{ background: token.colorBgLayout }}>
          <Content style={{ margin: 16, padding: 24, background: token.colorBgContainer, minHeight: 360 }}>
            <Breadcrumb style={{ marginBottom: 16 }} items={breadcrumbItems} />
            <Outlet />
          </Content>
        </Layout>
      </Layout>
    </Layout>
  );
}

function AppLayout() {
  const sidebarMenuNonce = useAppStore((s) => s.sidebarMenuNonce);
  const navigate = useNavigate();
  const location = useLocation();
  const { message } = AntdAppProvider.useApp();
  const token = useAuthStore((state) => state.token);
  const user = useAuthStore((state) => state.user);
  const doLogout = useAuthStore((state) => state.logout);
  const refreshMe = useAuthStore((state) => state.refreshMe);
  const [dark, setDark] = useState(false);
  const runtimeValues = useRuntimeConfigStore((state) => state.values);
  const refreshRuntimeConfig = useRuntimeConfigStore((state) => state.refresh);
  const patchRuntimeConfig = useRuntimeConfigStore((state) => state.patchValue);
  const [menuItems, setMenuItems] = useState([]);
  const [pathTitleMap, setPathTitleMap] = useState({});
  const [menuOpenKeys, setMenuOpenKeys] = useState([]);
  const branding = appConfig.branding || {};

  const fallbackMenuItems = useMemo(
    () => [
      { key: "/", icon: <HomeOutlined />, label: <Link to="/">工作台</Link> },
      { key: "/messages", icon: <BellOutlined />, label: <Link to="/messages">消息中心</Link> },
      {
        key: "grp-system",
        label: "系统管理",
        children: [
          { key: "/users", icon: <UserOutlined />, label: <Link to="/users">用户管理</Link> },
          { key: "/orgs", icon: <ApartmentOutlined />, label: <Link to="/orgs">机构管理</Link> },
          { key: "/roles", icon: <SafetyCertificateOutlined />, label: <Link to="/roles">角色管理</Link> },
          { key: "/menus", icon: <MenuOutlined />, label: <Link to="/menus">菜单管理</Link> },
          { key: "/dicts", icon: <BookOutlined />, label: <Link to="/dicts">字典管理</Link> },
          { key: "/configs", icon: <SettingOutlined />, label: <Link to="/configs">配置中心</Link> },
          { key: "/sessions", icon: <LockOutlined />, label: <Link to="/sessions">会话管理</Link> },
          { key: "/audits", icon: <AuditOutlined />, label: <Link to="/audits">审计管理</Link> },
          { key: "/logs", icon: <FileSearchOutlined />, label: <Link to="/logs">日志管理</Link> },
          { key: "/runtime-logs", icon: <MonitorOutlined />, label: <Link to="/runtime-logs">运行日志</Link> },
        ],
      },
      {
        key: "grp-platform",
        label: "平台运营",
        children: [
          { key: "/platform/tenants", icon: <BankOutlined />, label: <Link to="/platform/tenants">租户管理</Link> },
          { key: "/platform/accounts", icon: <UserOutlined />, label: <Link to="/platform/accounts">平台账号</Link> },
          { key: "/platform/roles", icon: <SafetyCertificateOutlined />, label: <Link to="/platform/roles">平台角色</Link> },
          { key: "/platform/audits", icon: <FileTextOutlined />, label: <Link to="/platform/audits">平台审计</Link> },
          { key: "/platform/announcements", icon: <BellOutlined />, label: <Link to="/platform/announcements">平台公告</Link> },
          { key: "/platform/configs", icon: <SettingOutlined />, label: <Link to="/platform/configs">平台配置</Link> },
          { key: "/platform/assist", icon: <ToolOutlined />, label: <Link to="/platform/assist">跨租户协助</Link> },
          { key: "/platform/logs", icon: <FileTextOutlined />, label: <Link to="/platform/logs">平台日志</Link> },
        ],
      },
    ],
    [],
  );

  const platformNavSource = useMemo(
    () => [
      { key: "/", icon: <HomeOutlined />, label: <Link to="/">工作台</Link> },
      {
        key: "grp-platform",
        label: "平台运营",
        children: [
          { key: "/platform/tenants", perm: "platform.tenant.read", icon: <BankOutlined />, label: <Link to="/platform/tenants">租户管理</Link> },
          { key: "/platform/accounts", perm: "platform.account.read", icon: <UserOutlined />, label: <Link to="/platform/accounts">平台账号</Link> },
          {
            key: "/platform/roles",
            perm: "platform.role.read",
            icon: <SafetyCertificateOutlined />,
            label: <Link to="/platform/roles">平台角色</Link>,
          },
          { key: "/platform/audits", perm: "platform.audit.read", icon: <FileTextOutlined />, label: <Link to="/platform/audits">平台审计</Link> },
          {
            key: "/platform/announcements",
            perm: "platform.announcement.read",
            icon: <BellOutlined />,
            label: <Link to="/platform/announcements">平台公告</Link>,
          },
          {
            key: "/platform/configs",
            perm: "platform.config.read",
            icon: <SettingOutlined />,
            label: <Link to="/platform/configs">平台配置</Link>,
          },
          {
            key: "/platform/assist",
            perm: ["platform.assist.force_logout_user", "platform.assist.permission_trace"],
            icon: <ToolOutlined />,
            label: <Link to="/platform/assist">跨租户协助</Link>,
          },
          {
            key: "/platform/logs",
            perm: "platform.log.read",
            icon: <FileTextOutlined />,
            label: <Link to="/platform/logs">平台日志</Link>,
          },
        ],
      },
    ],
    [],
  );

  const platformMenuItems = useMemo(
    () => filterPlatformMenuItems(platformNavSource, user),
    [platformNavSource, user],
  );

  const principalType = user?.principalType;
  const isPlatform = principalType === "PLATFORM_ACCOUNT";

  useEffect(() => {
    if (!token) {
      clearAccessTokenExpirySchedule();
      return;
    }
    const raw = localStorage.getItem("tokenExpiresAt");
    if (raw) {
      const t = Number(raw);
      if (Number.isFinite(t) && Date.now() >= t) {
        clearAccessTokenExpirySchedule();
        localStorage.removeItem("accessToken");
        localStorage.removeItem("tokenExpiresAt");
        navigate("/login", { replace: true });
        return;
      }
      if (Number.isFinite(t)) {
        const remainSec = Math.max(1, Math.floor((t - Date.now()) / 1000));
        scheduleAccessTokenExpiry(remainSec);
      }
    }
    if (!isPlatform) {
      refreshRuntimeConfig();
    }
  }, [token, refreshRuntimeConfig, navigate, isPlatform]);

  useEffect(() => {
    if (!token || !isPlatform) return;
    refreshMe();
  }, [token, isPlatform, refreshMe]);

  useEffect(() => {
    const themeValue = String(runtimeValues["ui.theme.default"] || "light").toLowerCase();
    const nextDark = themeValue === "dark";
    setDark(nextDark);
    document.documentElement.setAttribute("data-theme", nextDark ? "dark" : "light");
  }, [runtimeValues]);

  useEffect(() => {
    if (!token) return;
    if (isPlatform) {
      setMenuOpenKeys([]);
      setMenuItems(platformMenuItems);
      setPathTitleMap(PATH_TITLE);
      return;
    }
    let cancelled = false;
    const buildMenuItems = (nodes, titleMap, parentPath = "") => {
      const items = [];
      (nodes || []).forEach((node) => {
        if (node.hidden) {
          items.push(...buildMenuItems(node.children, titleMap, parentPath));
          return;
        }
        const id = String(node.menuId || "");
        const path = node.path ? String(node.path) : "";
        const key = path || `${parentPath}/m-${id}`;
        const item = {
          key,
          label: path ? <Link to={path}>{node.name || key}</Link> : (node.name || key),
        };
        const iconEl = getMenuIcon(node.icon);
        if (iconEl) item.icon = iconEl;
        if (path) {
          titleMap[path] = node.name || path;
        }
        if (node.children && node.children.length) {
          item.children = buildMenuItems(node.children, titleMap, key);
        }
        items.push(item);
      });
      return items;
    };
    (async () => {
      try {
        const res = await queryMyMenus({ _t: sidebarMenuNonce });
        const nodes = res.data?.data?.items || [];
        const titleMap = { ...PATH_TITLE };
        const dynamic = buildMenuItems(nodes, titleMap);
        const nextItems = [{ key: "/", icon: <HomeOutlined />, label: <Link to="/">工作台</Link> }].concat(dynamic);
        if (!cancelled) {
          setMenuOpenKeys([]);
          setMenuItems(nextItems);
          setPathTitleMap(titleMap);
        }
      } catch {
        if (!cancelled) {
          setMenuOpenKeys([]);
          setMenuItems(fallbackMenuItems);
          setPathTitleMap(PATH_TITLE);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token, fallbackMenuItems, sidebarMenuNonce, isPlatform, platformMenuItems]);

  const selectedKeys = useMemo(() => {
    const p = location.pathname;
    if (p === "/") return ["/"];
    const keys = Object.keys(pathTitleMap).filter((k) => k !== "/" && (p === k || p.startsWith(k + "/")));
    const hit = keys.sort((a, b) => b.length - a.length)[0];
    return hit ? [hit] : [];
  }, [location.pathname, pathTitleMap]);

  const selectedMenuKey = selectedKeys[0];

  useEffect(() => {
    if (!menuItems.length) return;
    if (!selectedMenuKey || selectedMenuKey === "/") return;
    const pathKeys = findKeyPathFromItems(menuItems, selectedMenuKey);
    if (!pathKeys || pathKeys.length <= 1) return;
    const ancestors = pathKeys.slice(0, -1);
    setMenuOpenKeys((prev) => Array.from(new Set([...prev, ...ancestors])));
  }, [selectedMenuKey, menuItems]);

  const breadcrumbItems = useMemo(() => {
    const p = location.pathname;
    const cur = pathTitleMap[p] || p;
    if (p === "/") return [{ title: "工作台" }];
    const keys = Object.keys(pathTitleMap).filter((k) => k !== "/" && (p === k || p.startsWith(k + "/")));
    const hit = keys.sort((a, b) => b.length - a.length)[0];
    const group = PATH_GROUP[hit];
    if (group) return [{ title: group }, { title: cur }];
    return [{ title: <Link to="/">工作台</Link> }, { title: cur }];
  }, [location.pathname, pathTitleMap]);

  const pingBackend = async () => {
    try {
      const { data } = await axios.get(`${appConfig.apiBaseUrl}/api/ping`);
      message.success((data && data.data && data.data.message) || "ok");
    } catch {
      message.error("backend unavailable");
    }
  };

  const onLogout = async () => {
    const goPlatform = user?.principalType === "PLATFORM_ACCOUNT";
    await doLogout();
    navigate(goPlatform ? "/platform/login" : "/login", { replace: true });
  };

  const toggleTheme = () => {
    const next = !dark;
    patchRuntimeConfig("ui.theme.default", next ? "dark" : "light");
  };

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  const path = location.pathname;
  if (isPlatform && path !== "/" && !path.startsWith("/platform")) {
    return <Navigate to="/platform/tenants" replace />;
  }
  if (!isPlatform && path.startsWith("/platform")) {
    return <Navigate to="/" replace />;
  }

  const displayName = (user && user.displayName) || "管理员";
  const avatarChar = displayName.slice(0, 1);

  const userMenuItems = isPlatform
    ? [
        {
          key: "logout",
          icon: <LogoutOutlined />,
          danger: true,
          label: "退出登录",
          onClick: onLogout,
        },
      ]
    : [
        {
          key: "profile",
          icon: <ProfileOutlined />,
          label: "个人设置",
          onClick: () => navigate("/profile"),
        },
        { type: "divider" },
        {
          key: "logout",
          icon: <LogoutOutlined />,
          danger: true,
          label: "退出登录",
          onClick: onLogout,
        },
      ];

  return (
    <ConfigProvider
      theme={{
        algorithm: dark ? antdThemeApi.darkAlgorithm : antdThemeApi.defaultAlgorithm,
        ...(dark ? darkNeutralChromeOverrides : {}),
      }}
    >
      <ThemedShell
        dark={dark}
        branding={branding}
        menuItems={menuItems}
        selectedKeys={selectedKeys}
        menuOpenKeys={menuOpenKeys}
        onMenuOpenChange={setMenuOpenKeys}
        breadcrumbItems={breadcrumbItems}
        displayName={displayName}
        avatarChar={avatarChar}
        userMenuItems={userMenuItems}
        pingBackend={pingBackend}
        toggleTheme={toggleTheme}
        showMessagesLink={!isPlatform}
      />
    </ConfigProvider>
  );
}

export default function App() {
  return <AppLayout />;
}
