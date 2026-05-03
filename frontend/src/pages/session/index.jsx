import { useCallback, useEffect, useState } from "react";
import { DesktopOutlined, ReloadOutlined, SafetyCertificateOutlined } from "@ant-design/icons";
import { Alert, Button, Dropdown, Modal, Space, Table, Tag, Typography, theme, message } from "antd";
import { getSessions, revokeSession } from "../../api/auth";
import PageShell from "../../components/page/PageShell";
import TableCard from "../../components/page/TableCard";
import { clearAccessTokenExpirySchedule } from "../../store/token-expiry";
import { formatTs } from "../../utils/tableDisplay";

function currentSessionIdFromStorage() {
  const raw = localStorage.getItem("accessToken") || "";
  const t = raw.trim();
  if (!t.startsWith("token_")) {
    return null;
  }
  const id = t.slice("token_".length);
  return id || null;
}

function shortenSessionId(id) {
  if (!id || id.length < 14) return id || "—";
  return `${id.slice(0, 8)}…${id.slice(-4)}`;
}

function SessionPage() {
  const { token } = theme.useToken();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await getSessions({ page, pageSize });
      const data = response.data?.data || {};
      setItems(Array.isArray(data.items) ? data.items : []);
      setTotal(Number(data.total) || 0);
    } catch (e) {
      message.error(e?.response?.data?.message || e?.message || "加载失败");
    } finally {
      setLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => {
    load();
  }, [load]);

  const afterRevoke = (sessionId) => {
    if (sessionId && sessionId === currentSessionIdFromStorage()) {
      clearAccessTokenExpirySchedule();
      localStorage.removeItem("accessToken");
      localStorage.removeItem("tokenExpiresAt");
      window.location.href = "/login";
      return;
    }
    message.success("已下线");
    load();
  };

  const onRevoke = (sessionId) => {
    const isCurrent = sessionId === currentSessionIdFromStorage();
    Modal.confirm({
      title: "确认下线该会话？",
      content: isCurrent ? "将退出当前浏览器登录。" : "该终端将立即无法使用当前登录状态。",
      okText: "下线",
      okType: "danger",
      onOk: async () => {
        try {
          await revokeSession(sessionId);
          afterRevoke(sessionId);
        } catch (e) {
          message.error(e?.response?.data?.message || e?.message || "下线失败");
        }
      },
    });
  };

  const columns = [
    {
      title: "会话",
      dataIndex: "sessionId",
      width: 148,
      render: (id, r) => (
        <Space size={6} align="start">
          <DesktopOutlined style={{ color: r.current ? token.colorPrimary : token.colorTextSecondary, marginTop: 3 }} />
          <Typography.Text
            code
            copyable={{ text: id }}
            style={{ fontSize: 12, color: r.current ? token.colorPrimary : undefined }}
            ellipsis={{ tooltip: id }}
          >
            {shortenSessionId(id)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: "创建时间",
      dataIndex: "createdAt",
      width: 172,
      render: (v) => <Typography.Text type="secondary">{formatTs(v)}</Typography.Text>,
    },
    {
      title: "最近活跃",
      dataIndex: "lastSeenAt",
      width: 172,
      render: (v) => <Typography.Text type="secondary">{formatTs(v)}</Typography.Text>,
    },
    {
      title: "网络",
      key: "net",
      width: 132,
      render: (_, r) => (
        <Typography.Text code style={{ fontSize: 12 }}>
          {r.ip || "—"}
        </Typography.Text>
      ),
    },
    {
      title: "浏览器 / 设备",
      dataIndex: "userAgent",
      ellipsis: true,
      render: (v) => (
        <Typography.Text type="secondary" ellipsis={{ tooltip: v }} style={{ maxWidth: 280 }}>
          {v || "—"}
        </Typography.Text>
      ),
    },
    {
      title: "状态",
      width: 96,
      align: "center",
      render: (_, r) =>
        r.current ? (
          <Tag
            style={{
              margin: 0,
              border: `1px solid ${token.colorPrimary}`,
              color: token.colorPrimary,
              background: token.colorPrimaryBg,
            }}
          >
            当前设备
          </Tag>
        ) : (
          <Tag style={{ margin: 0 }}>其他</Tag>
        ),
    },
    {
      title: "操作",
      width: 96,
      fixed: "right",
      align: "center",
      render: (_, r) => (
        <Dropdown
          menu={{
            items: [{ key: "revoke", label: "下线", danger: true }],
            onClick: ({ key }) => {
              if (key === "revoke") onRevoke(r.sessionId);
            },
          }}
          trigger={["click"]}
        >
          <Button type="link" size="small" style={{ padding: 0 }}>
            更多
          </Button>
        </Dropdown>
      ),
    },
  ];

  return (
    <PageShell
      title="会话管理"
      subTitle="查看当前账号已登录的终端；可下线非本人或可疑会话，当前浏览器下线后将跳转登录页。"
      extra={
        <Button icon={<ReloadOutlined />} onClick={() => load()} loading={loading}>
          刷新
        </Button>
      }
    >
      <Alert
        type="info"
        showIcon
        icon={<SafetyCertificateOutlined />}
        message="安全提示"
        description="列表仅展示您本人、本租户内的有效会话；下线后立即失效，无需修改密码。"
        style={{
          marginBottom: 16,
          borderRadius: 8,
          border: `1px solid ${token.colorBorderSecondary}`,
          background: token.colorFillAlter,
        }}
      />
      <TableCard
        title="登录会话"
        extra={
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            共 {total} 条
          </Typography.Text>
        }
        rowKey="sessionId"
        loading={loading}
        columns={columns}
        dataSource={items}
        size="middle"
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          pageSizeOptions: ["10", "20", "50"],
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p, ps) => {
            setPage(p);
            setPageSize(ps);
          },
        }}
        scroll={{ x: 960 }}
      />
    </PageShell>
  );
}

export default SessionPage;
