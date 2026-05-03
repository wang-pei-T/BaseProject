import { useCallback, useEffect, useMemo, useState } from "react";
import { Button, DatePicker, Form, Input, Table } from "antd";
import { exportTenantLogsCsv, queryTenantLogs } from "../../api/tenantLog";
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

function TenantLogPage() {
  const pageSize = Number(useRuntimeConfigStore((s) => s.values["ui.page.defaultSize"]) || 20);
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [draft, setDraft] = useState({ level: "", module: "", action: "", requestId: "" });
  const [rangeDraft, setRangeDraft] = useState(null);
  const [q, setQ] = useState({ level: "", module: "", action: "", requestId: "", from: "", to: "" });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, pageSize };
      if (q.level) params.level = q.level;
      if (q.module) params.module = q.module;
      if (q.action) params.action = q.action;
      if (q.requestId) params.requestId = q.requestId;
      if (q.from) params.from = q.from;
      if (q.to) params.to = q.to;
      const res = await queryTenantLogs(params);
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
    if (q.level) params.level = q.level;
    if (q.module) params.module = q.module;
    if (q.action) params.action = q.action;
    if (q.requestId) params.requestId = q.requestId;
    if (q.from) params.from = q.from;
    if (q.to) params.to = q.to;
    return params;
  }, [q]);

  const onExport = async () => {
    setExporting(true);
    try {
      const res = await exportTenantLogsCsv(exportParams);
      downloadBlob(res, "tenant-logs.csv");
    } finally {
      setExporting(false);
    }
  };

  const columns = [
    { title: "级别", dataIndex: "level", width: 80 },
    { title: "模块", dataIndex: "module", width: 120 },
    { title: "动作", dataIndex: "action", width: 120 },
    { title: "消息", dataIndex: "message", ellipsis: true },
    { title: "请求ID", dataIndex: "requestId", width: 140, ellipsis: true },
    { title: "时间", dataIndex: "createdAt", width: 200 },
  ];

  return (
    <PageShell title="日志管理">
      <QueryBar
        loading={loading}
        onSearch={() => {
          const from = rangeDraft?.[0]?.toISOString() || "";
          const to = rangeDraft?.[1]?.toISOString() || "";
          setQ({ ...draft, from, to });
          setPage(1);
        }}
        onReset={() => {
          const z = { level: "", module: "", action: "", requestId: "", from: "", to: "" };
          setDraft({ level: "", module: "", action: "", requestId: "" });
          setRangeDraft(null);
          setQ(z);
          setPage(1);
        }}
      >
        <Form.Item label="级别">
          <Input value={draft.level} onChange={(e) => setDraft({ ...draft, level: e.target.value })} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item label="模块">
          <Input value={draft.module} onChange={(e) => setDraft({ ...draft, module: e.target.value })} style={{ width: 140 }} />
        </Form.Item>
        <Form.Item label="动作">
          <Input value={draft.action} onChange={(e) => setDraft({ ...draft, action: e.target.value })} style={{ width: 140 }} />
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
        rowKey="logId"
        loading={loading}
        columns={columns}
        dataSource={items}
        scroll={{ x: 960 }}
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

export default TenantLogPage;
