import { useCallback, useEffect, useState } from "react";
import { Form, Input, Table } from "antd";
import useAuthStore from "../../store/auth";
import { platformHasPerm } from "../../utils/platform-perm";
import { queryPlatformAudits } from "../../api/platformAudit";
import PageShell from "../../components/page/PageShell";
import QueryBar from "../../components/page/QueryBar";

function PlatformAuditPage() {
  const user = useAuthStore((s) => s.user);
  const can = (c) => platformHasPerm(user, c);
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(30);
  const [loading, setLoading] = useState(false);
  const [q, setQ] = useState({ event: "", targetTenantId: "", operatorPlatformUserId: "", from: "", to: "" });
  const [draft, setDraft] = useState({ event: "", targetTenantId: "", operatorPlatformUserId: "", from: "", to: "" });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, pageSize };
      if (q.event) params.event = q.event;
      if (q.targetTenantId) params.targetTenantId = q.targetTenantId;
      if (q.operatorPlatformUserId) params.operatorPlatformUserId = q.operatorPlatformUserId;
      if (q.from) params.from = q.from;
      if (q.to) params.to = q.to;
      const res = await queryPlatformAudits(params);
      const data = res.data?.data || {};
      setItems(data.items || []);
      setTotal(Number(data.total) || 0);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, q]);

  useEffect(() => {
    load();
  }, [load]);

  const columns = [
    { title: "事件", dataIndex: "event", width: 220 },
    { title: "操作者", dataIndex: "operatorPlatformUserId", width: 120 },
    { title: "目标租户", dataIndex: "targetTenantId", width: 110 },
    { title: "请求ID", dataIndex: "requestId", ellipsis: true },
    { title: "时间", dataIndex: "createdAt", width: 200 },
  ];

  if (!can("platform.audit.read")) {
    return (
      <PageShell title="平台审计">
        <p>无权限</p>
      </PageShell>
    );
  }

  return (
    <PageShell title="平台审计">
      <QueryBar
        loading={loading}
        onSearch={() => {
          setQ({ ...draft });
          setPage(1);
        }}
        onReset={() => {
          const z = { event: "", targetTenantId: "", operatorPlatformUserId: "", from: "", to: "" };
          setDraft(z);
          setQ(z);
          setPage(1);
        }}
      >
        <Form.Item label="事件">
          <Input value={draft.event} onChange={(e) => setDraft({ ...draft, event: e.target.value })} style={{ width: 160 }} />
        </Form.Item>
        <Form.Item label="目标租户">
          <Input value={draft.targetTenantId} onChange={(e) => setDraft({ ...draft, targetTenantId: e.target.value })} style={{ width: 120 }} />
        </Form.Item>
        <Form.Item label="操作者">
          <Input
            value={draft.operatorPlatformUserId}
            onChange={(e) => setDraft({ ...draft, operatorPlatformUserId: e.target.value })}
            style={{ width: 120 }}
          />
        </Form.Item>
        <Form.Item label="from(ISO)">
          <Input value={draft.from} onChange={(e) => setDraft({ ...draft, from: e.target.value })} style={{ width: 200 }} />
        </Form.Item>
        <Form.Item label="to(ISO)">
          <Input value={draft.to} onChange={(e) => setDraft({ ...draft, to: e.target.value })} style={{ width: 200 }} />
        </Form.Item>
      </QueryBar>
      <Table
        rowKey="auditId"
        loading={loading}
        columns={columns}
        dataSource={items}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: false,
          onChange: (p) => setPage(p),
        }}
      />
    </PageShell>
  );
}

export default PlatformAuditPage;
