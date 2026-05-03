import { Link } from "react-router-dom";
import { Card, Col, Row, Statistic, theme } from "antd";
import PageShell from "../../components/page/PageShell";
import useAuthStore from "../../store/auth";

const tenantMessageTiles = [
  { to: "/messages", title: "消息中心", desc: "站内信与已读状态", icon: "💬", color: "#1677ff" },
];

const tenantSystemTiles = [
  { to: "/users", title: "用户管理", desc: "租户内用户全生命周期", icon: "👤", color: "#008c8c" },
  { to: "/orgs", title: "机构管理", desc: "机构树、启停与人员批量调整", icon: "🏢", color: "#1677ff" },
  { to: "/roles", title: "角色管理", desc: "角色与权限绑定", icon: "🔑", color: "#52c41a" },
  { to: "/menus", title: "菜单管理", desc: "菜单树与权限", icon: "📋", color: "#fa8c16" },
  { to: "/dicts", title: "字典管理", desc: "字典类型与字典项", icon: "📖", color: "#722ed1" },
  { to: "/configs", title: "配置中心", desc: "租户配置项", icon: "⚙️", color: "#8c8c8c" },
  { to: "/sessions", title: "会话管理", desc: "在线会话与下线", icon: "🔐", color: "#8c8c8c" },
  { to: "/audits", title: "审计管理", desc: "操作审计与详情追溯", icon: "📝", color: "#008c8c" },
  { to: "/logs", title: "日志管理", desc: "系统运行与业务日志", icon: "📋", color: "#595959" },
];

const tenantOtherTiles = [{ to: "/profile", title: "个人中心", desc: "资料与密码", icon: "👤", color: "#008c8c" }];

const platformEntries = [
  { to: "/platform/tenants", title: "租户管理", desc: "平台侧租户", icon: "🏦", color: "#fa8c16" },
  { to: "/platform/accounts", title: "平台账号", desc: "PACC", icon: "👤", color: "#1677ff" },
  { to: "/platform/roles", title: "平台角色", desc: "PROLE", icon: "🔑", color: "#52c41a" },
  { to: "/platform/audits", title: "平台审计", desc: "PAUD", icon: "📝", color: "#008c8c" },
  { to: "/platform/announcements", title: "平台公告", desc: "PANN", icon: "📢", color: "#fa8c16" },
  { to: "/platform/configs", title: "平台配置", desc: "CONF", icon: "⚙️", color: "#8c8c8c" },
  { to: "/platform/assist", title: "跨租户协助", desc: "ASSIST", icon: "🛠️", color: "#722ed1" },
  { to: "/platform/logs", title: "平台日志", desc: "PLOG", icon: "📋", color: "#595959" },
];

function TileCard({ e }) {
  const { token } = theme.useToken();
  return (
    <Col xs={24} sm={12} md={8} lg={6} key={e.to}>
      <Link to={e.to} style={{ textDecoration: "none", color: "inherit" }}>
        <Card hoverable size="small">
          <div style={{ display: "flex", gap: 12 }}>
            <div style={{ fontSize: 28, lineHeight: 1 }}>{e.icon}</div>
            <div>
              <div style={{ fontWeight: 600, color: e.color }}>{e.title}</div>
              <div style={{ fontSize: 12, color: token.colorTextSecondary, marginTop: 4 }}>{e.desc}</div>
              <div style={{ fontSize: 12, color: token.colorPrimary, marginTop: 8 }}>进入 →</div>
            </div>
          </div>
        </Card>
      </Link>
    </Col>
  );
}

function HomePage() {
  const user = useAuthStore((s) => s.user);
  const isPlatform = user?.principalType === "PLATFORM_ACCOUNT";
  return (
    <PageShell title="工作台">
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} md={6}>
          <Card size="small">
            <Statistic title="用户总数" value="—" />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card size="small">
            <Statistic title="角色总数" value="—" />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card size="small">
            <Statistic title="机构总数" value="—" />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card size="small">
            <Statistic title="今日审计" value="—" />
          </Card>
        </Col>
      </Row>
      {isPlatform ? (
        <>
          <div style={{ fontWeight: 600, marginBottom: 12 }}>平台运营</div>
          <Row gutter={[16, 16]}>
            {platformEntries.map((e) => (
              <TileCard key={e.to} e={e} />
            ))}
          </Row>
        </>
      ) : (
        <>
          <div style={{ fontWeight: 600, marginBottom: 12 }}>消息中心</div>
          <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
            {tenantMessageTiles.map((e) => (
              <TileCard key={e.to} e={e} />
            ))}
          </Row>
          <div style={{ fontWeight: 600, marginBottom: 12 }}>系统管理</div>
          <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
            {tenantSystemTiles.map((e) => (
              <TileCard key={e.to} e={e} />
            ))}
          </Row>
          <div style={{ fontWeight: 600, marginBottom: 12 }}>其他</div>
          <Row gutter={[16, 16]}>
            {tenantOtherTiles.map((e) => (
              <TileCard key={e.to} e={e} />
            ))}
          </Row>
        </>
      )}
    </PageShell>
  );
}

export default HomePage;
