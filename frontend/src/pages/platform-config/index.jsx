import { useCallback, useEffect, useState } from "react";
import { Button, Form, Input, Modal, Table, Typography, message } from "antd";
import useAuthStore from "../../store/auth";
import { platformHasPerm } from "../../utils/platform-perm";
import { queryPlatformConfigs, updatePlatformConfig } from "../../api/config";
import PageShell from "../../components/page/PageShell";

function PlatformConfigPage() {
  const user = useAuthStore((s) => s.user);
  const can = (c) => platformHasPerm(user, c);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [activeRow, setActiveRow] = useState(null);
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await queryPlatformConfigs({ page: 1, pageSize: 200 });
      setItems(res.data?.data?.items || []);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openEdit = (row) => {
    setActiveRow(row);
    form.setFieldsValue({ value: row.value ?? "" });
    setEditOpen(true);
  };

  const columns = [
    { title: "键", dataIndex: "key", width: 260 },
    { title: "值", dataIndex: "value", ellipsis: true },
    { title: "版本", dataIndex: "version", width: 80 },
    {
      title: "操作",
      width: 100,
      render: (_, row) =>
        can("platform.config.update") ? (
          <Button type="link" size="small" onClick={() => openEdit(row)}>
            编辑
          </Button>
        ) : null,
    },
  ];

  return (
    <PageShell title="平台配置">
      <Table rowKey="key" loading={loading} columns={columns} dataSource={items} pagination={false} />
      <Modal
        title={`编辑 ${activeRow?.key || ""}`}
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={async () => {
          try {
            const v = await form.validateFields();
            await updatePlatformConfig(activeRow.key, { value: v.value, expectedVersion: activeRow.version });
            message.success("已保存");
            setEditOpen(false);
            load();
          } catch (e) {
            if (e?.errorFields) return;
            message.error(String(e?.response?.data?.message || e?.message || "失败"));
          }
        }}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item label="当前版本">
            <Typography.Text>{activeRow?.version}</Typography.Text>
          </Form.Item>
          <Form.Item name="value" label="新值" rules={[{ required: true }]}>
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </PageShell>
  );
}

export default PlatformConfigPage;
