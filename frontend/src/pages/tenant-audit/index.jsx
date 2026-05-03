import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  DatePicker,
  Descriptions,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Typography,
} from "antd";
import { exportTenantAuditsCsv, getTenantAudit, queryTenantAudits } from "../../api/tenantAudit";
import PageShell from "../../components/page/PageShell";
import QueryBar from "../../components/page/QueryBar";
import useRuntimeConfigStore from "../../store/runtime-config";

function downloadBlob(res, filename) {
  const blob = new Blob([res.data], { type: "text/csv;charset=utf-8" });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  window.URL.revokeObjectURL(url);
}

function DiffBlock({ text }) {
  if (text == null || text === "") return <Typography.Text type="secondary">（无）</Typography.Text>;
  if (typeof text !== "string") {
    return <pre style={{ whiteSpace: "pre-wrap", margin: 0 }}>{JSON.stringify(text, null, 2)}</pre>;
  }
  try {
    const o = JSON.parse(text);
    if (o && typeof o === "object" && !Array.isArray(o)) {
      const rows = Object.keys(o).map((k) => ({ k, v: String(o[k]) }));
      return (
        <Table
          size="small"
          rowKey="k"
          pagination={false}
          dataSource={rows}
          columns={[
            { title: "键", dataIndex: "k", width: 160 },
            { title: "值", dataIndex: "v", ellipsis: true },
          ]}
        />
      );
    }
  } catch {
    /* fallthrough */
  }
  return <pre style={{ whiteSpace: "pre-wrap", margin: 0, fontFamily: "monospace" }}>{text}</pre>;
}

function TenantAuditPage() {
  const pageSize = Number(useRuntimeConfigStore((s) => s.values["ui.page.defaultSize"]) || 20);
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [detail, setDetail] = useState(null);
  const [draft, setDraft] = useState({ event: "", operatorUserId: "", targetId: "", requestId: "" });
  const [rangeDraft, setRangeDraft] = useState(null);
  const [q, setQ] = useState({
    event: "",
    operatorUserId: "",
    targetId: "",
    requestId: "",
    from: "",
    to: "",
  });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, pageSize };
      if (q.event) params.event = q.event;
      if (q.operatorUserId) params.operatorUserId = q.operatorUserId;
      if (q.targetId) params.targetId = q.targetId;
      if (q.requestId) params.requestId = q.requestId;
      if (q.from) params.from = q.from;
      if (q.to) params.to = q.to;
      const res = await queryTenantAudits(params);
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

  const exportParams = useMemo(() => {
    const params = {};
    if (q.event) params.event = q.event;
    if (q.operatorUserId) params.operatorUserId = q.operatorUserId;
    if (q.targetId) params.targetId = q.targetId;
    if (q.requestId) params.requestId = q.requestId;
    if (q.from) params.from = q.from;
    if (q.to) params.to = q.to;
    return params;
  }, [q]);

  const onExport = async () => {
    setExporting(true);
    try {
      const res = await exportTenantAuditsCsv(exportParams);
      downloadBlob(res, "tenant-audits.csv");
    } finally {
      setExporting(false);
    }
  };

  const openDetail = async (id) => {
    const res = await getTenantAudit(id);
    setDetail(res.data?.data || null);
  };

  const columns = [
    { title: "事件", dataIndex: "event", width: 200 },
    { title: "操作人", dataIndex: "operatorUserId", width: 110 },
    { title: "目标", dataIndex: "targetId", width: 120, ellipsis: true },
    { title: "请求ID", dataIndex: "requestId", ellipsis: true },
    { title: "时间", dataIndex: "createdAt", width: 200 },
    {
      title: "操作",
      width: 80,
      fixed: "right",
      render: (_, r) => (
        <a role="presentation" onClick={() => openDetail(r.auditId)}>
          详情
        </a>
      ),
    },
  ];

  return (
    <PageShell title="审计管理">
      <QueryBar
        loading={loading}
        onSearch={() => {
          const from = rangeDraft?.[0]?.toISOString() || "";
          const to = rangeDraft?.[1]?.toISOString() || "";
          setQ({ ...draft, from, to });
          setPage(1);
        }}
        onReset={() => {
          const z = { event: "", operatorUserId: "", targetId: "", requestId: "", from: "", to: "" };
          setDraft({ event: "", operatorUserId: "", targetId: "", requestId: "" });
          setRangeDraft(null);
          setQ(z);
          setPage(1);
        }}
      >
        <Form.Item label="事件">
          <Input value={draft.event} onChange={(e) => setDraft({ ...draft, event: e.target.value })} style={{ width: 160 }} />
        </Form.Item>
        <Form.Item label="操作人ID">
          <Input
            value={draft.operatorUserId}
            onChange={(e) => setDraft({ ...draft, operatorUserId: e.target.value })}
            style={{ width: 120 }}
          />
        </Form.Item>
        <Form.Item label="目标ID">
          <Input value={draft.targetId} onChange={(e) => setDraft({ ...draft, targetId: e.target.value })} style={{ width: 120 }} />
        </Form.Item>
        <Form.Item label="请求ID">
          <Input value={draft.requestId} onChange={(e) => setDraft({ ...draft, requestId: e.target.value })} style={{ width: 160 }} />
        </Form.Item>
        <Form.Item label="时间范围">
          <DatePicker.RangePicker showTime value={rangeDraft} onChange={setRangeDraft} style={{ width: 360 }} />
        </Form.Item>
        <Form.Item>
          <Button onClick={onExport} loading={exporting}>
            导出 CSV
          </Button>
        </Form.Item>
      </QueryBar>
      <Table
        rowKey="auditId"
        loading={loading}
        columns={columns}
        dataSource={items}
        scroll={{ x: 900 }}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: false,
          onChange: (p) => setPage(p),
        }}
      />
      <Modal open={!!detail} onCancel={() => setDetail(null)} footer={null} width={800} title="审计详情">
        {detail ? (
          <Space direction="vertical" size="middle" style={{ width: "100%" }}>
            <Descriptions bordered size="small" column={1}>
              <Descriptions.Item label="事件">{detail.event}</Descriptions.Item>
              <Descriptions.Item label="操作人ID">{detail.operatorUserId}</Descriptions.Item>
              <Descriptions.Item label="目标ID">{detail.targetId}</Descriptions.Item>
              <Descriptions.Item label="请求ID">{detail.requestId}</Descriptions.Item>
              <Descriptions.Item label="时间">{detail.createdAt}</Descriptions.Item>
              <Descriptions.Item label="操作 IP">{detail.operatorIp || "—"}</Descriptions.Item>
              <Descriptions.Item label="UA">{detail.userAgent || "—"}</Descriptions.Item>
            </Descriptions>
            <div>
              <Typography.Text strong>变更 diff</Typography.Text>
              <div style={{ marginTop: 8 }}>
                <DiffBlock text={detail.diff} />
              </div>
            </div>
            <div>
              <Typography.Text strong>上下文</Typography.Text>
              <Typography.Paragraph style={{ marginTop: 8, marginBottom: 0 }}>
                <pre style={{ whiteSpace: "pre-wrap", margin: 0, fontFamily: "monospace" }}>
                  {detail.context || "（无）"}
                </pre>
              </Typography.Paragraph>
            </div>
          </Space>
        ) : null}
      </Modal>
    </PageShell>
  );
}

export default TenantAuditPage;
