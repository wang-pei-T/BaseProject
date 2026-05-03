import { useCallback, useEffect, useState } from "react";
import { Button, Card, Dropdown, Form, Input, Modal, Select, Space, Table, Upload, message } from "antd";
import { getMessage, markAllMessagesRead, markMessageRead, queryMessages } from "../../api/message";
import { uploadFile } from "../../api/file";
import { bindAttachment, queryAttachments } from "../../api/attachment";
import PageShell from "../../components/page/PageShell";
import QueryBar from "../../components/page/QueryBar";
import TableCard from "../../components/page/TableCard";
import useRuntimeConfigStore from "../../store/runtime-config";

function uploadAllowed(file, values) {
  const maxMb = Number(values["file.upload.maxSizeMB"] || 20);
  const maxBytes = maxMb * 1024 * 1024;
  if (file.size > maxBytes) {
    message.error("文件超过租户配置的大小上限");
    return false;
  }
  const allowed = String(values["file.upload.allowedTypes"] || "jpg,png,pdf");
  const name = file.name || "";
  const dot = name.lastIndexOf(".");
  const ext = dot >= 0 ? name.slice(dot + 1).toLowerCase() : "";
  const parts = allowed.split(",");
  let ok = false;
  for (let i = 0; i < parts.length; i += 1) {
    let t = parts[i].trim().toLowerCase();
    if (t.startsWith(".")) {
      t = t.slice(1);
    }
    if (t && ext === t) {
      ok = true;
      break;
    }
  }
  if (!ok) {
    message.error("文件类型不在租户允许列表内");
    return false;
  }
  return true;
}

function MessagePage() {
  const runtimeValues = useRuntimeConfigStore((state) => state.values);
  const pageSize = Number(runtimeValues["ui.page.defaultSize"] || 20);
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState(null);
  const [attachItems, setAttachItems] = useState([]);
  const [q, setQ] = useState({ status: "", type: "" });
  const [draft, setDraft] = useState({ status: "", type: "" });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, pageSize };
      if (q.status) params.status = q.status;
      if (q.type) params.type = q.type;
      const res = await queryMessages(params);
      const data = res.data?.data || {};
      setItems(data.items || []);
      setTotal(Number(data.total) || 0);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, q]);

  const loadAttach = async () => {
    const res = await queryAttachments({ bizType: "DEMO", bizId: "1", page: 1, pageSize: Math.min(pageSize, 50) });
    setAttachItems(res.data?.data?.items || []);
  };

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    loadAttach();
  }, [pageSize]);

  const openDetail = async (id) => {
    const res = await getMessage(id);
    setDetail(res.data?.data || null);
  };

  const onUpload = async (file) => {
    if (!uploadAllowed(file, runtimeValues)) {
      return;
    }
    try {
      const up = await uploadFile(file, "message");
      const fileId = up.data?.data?.fileId;
      await bindAttachment({ fileId, bizType: "DEMO", bizId: "1" });
      message.success(`已上传 fileId=${fileId}`);
      loadAttach();
    } catch (err) {
      message.error(err?.response?.data?.message || "上传失败");
    }
    return false;
  };

  const msgColumns = [
    {
      title: "标题",
      dataIndex: "title",
      ellipsis: true,
      render: (t, r) => (
        <a role="presentation" onClick={() => openDetail(r.messageId)}>
          {t}
        </a>
      ),
    },
    { title: "类型", dataIndex: "type", width: 120 },
    { title: "状态", dataIndex: "status", width: 90 },
    { title: "时间", dataIndex: "createdAt", width: 200 },
    {
      title: "操作",
      width: 100,
      fixed: "right",
      render: (_, r) => (
        <Dropdown
          menu={{
            items: [
              { key: "detail", label: "详情" },
              { key: "read", label: "标记已读" },
            ],
            onClick: ({ key }) => {
              if (key === "detail") openDetail(r.messageId);
              if (key === "read") {
                markMessageRead(r.messageId).then(() => { message.success("已读"); load(); });
              }
            },
          }}
          trigger={["click"]}
        >
          <Button type="link" size="small">
            更多
          </Button>
        </Dropdown>
      ),
    },
  ];

  const attachColumns = [{ title: "文件名", dataIndex: "fileName" }];

  return (
    <PageShell
      title="消息中心"
      extra={
        <Space>
          <Upload beforeUpload={(f) => { onUpload(f); return false; }} showUploadList={false}>
            <Button>上传并绑定附件</Button>
          </Upload>
          <Button
            type="primary"
            onClick={() =>
              markAllMessagesRead(q.type ? { type: q.type } : {}).then(() => {
                message.success("已全部已读");
                load();
              })
            }
          >
            全部已读
          </Button>
        </Space>
      }
    >
      <QueryBar
        loading={loading}
        onSearch={() => {
          setQ({ ...draft });
          setPage(1);
        }}
        onReset={() => {
          const z = { status: "", type: "" };
          setDraft(z);
          setQ(z);
          setPage(1);
        }}
      >
        <Form.Item label="状态">
          <Select
            allowClear
            placeholder="全部"
            style={{ width: 120 }}
            value={draft.status || undefined}
            onChange={(v) => setDraft({ ...draft, status: v || "" })}
            options={[
              { value: "UNREAD", label: "未读" },
              { value: "READ", label: "已读" },
            ]}
          />
        </Form.Item>
        <Form.Item label="类型">
          <Input
            value={draft.type}
            onChange={(e) => setDraft({ ...draft, type: e.target.value })}
            style={{ width: 140 }}
            placeholder="如 SYSTEM"
          />
        </Form.Item>
      </QueryBar>
      <TableCard
        title="站内信"
        rowKey="messageId"
        loading={loading}
        columns={msgColumns}
        dataSource={items}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: false,
          onChange: (p) => setPage(p),
        }}
      />
      <Modal open={!!detail} onCancel={() => setDetail(null)} footer={null} width={720} title="消息详情">
        <pre style={{ whiteSpace: "pre-wrap", margin: 0 }}>{detail ? JSON.stringify(detail, null, 2) : ""}</pre>
      </Modal>
      <Card size="small" title="演示附件 DEMO/1" style={{ marginTop: 16 }}>
        <Table rowKey="attachmentId" size="small" columns={attachColumns} dataSource={attachItems} pagination={false} />
      </Card>
    </PageShell>
  );
}

export default MessagePage;
