import { useCallback, useEffect, useState } from "react";
import { Button, Dropdown, Form, Input, Modal, Select, Space, Table, message } from "antd";
import useAuthStore from "../../store/auth";
import { platformHasPerm } from "../../utils/platform-perm";
import {
  createAnnouncement,
  getAnnouncement,
  publishAnnouncement,
  queryAnnouncements,
  revokeAnnouncement,
} from "../../api/platformAnnouncement";
import PageShell from "../../components/page/PageShell";

function PlatformAnnouncementPage() {
  const user = useAuthStore((s) => s.user);
  const can = (c) => platformHasPerm(user, c);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState(null);
  const [revokeOpen, setRevokeOpen] = useState(false);
  const [revokeId, setRevokeId] = useState(null);
  const [form] = Form.useForm();
  const [revokeForm] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await queryAnnouncements({ page: 1, pageSize: 50 });
      setItems(res.data?.data?.items || []);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openDetail = async (id) => {
    setDetailOpen(true);
    try {
      const res = await getAnnouncement(id);
      setDetail(res.data?.data || null);
    } catch {
      setDetail(null);
    }
  };

  const columns = [
    { title: "标题", dataIndex: "title" },
    { title: "状态", dataIndex: "status", width: 120 },
    {
      title: "操作",
      width: 200,
      render: (_, row) => {
        const moreItems = [];
        if (row.status !== "PUBLISHED" && can("platform.announcement.publish")) {
          moreItems.push({ key: "publish", label: "发布" });
        }
        if (row.status === "PUBLISHED" && can("platform.announcement.revoke")) {
          moreItems.push({ key: "revoke", label: "撤回", danger: true });
        }
        const onMore = ({ key }) => {
          if (key === "publish") {
            Modal.confirm({
              title: "发布公告？",
              onOk: () => publishAnnouncement(row.announcementId).then(() => { message.success("已发布"); load(); }),
            });
          }
          if (key === "revoke") {
            setRevokeId(row.announcementId);
            revokeForm.resetFields();
            setRevokeOpen(true);
          }
        };
        return (
          <Space size={0} wrap>
            {can("platform.announcement.read") ? (
              <Button type="link" size="small" onClick={() => openDetail(row.announcementId)}>
                详情
              </Button>
            ) : null}
            {moreItems.length ? (
              <Dropdown menu={{ items: moreItems, onClick: onMore }} trigger={["click"]}>
                <Button type="link" size="small">
                  更多
                </Button>
              </Dropdown>
            ) : null}
          </Space>
        );
      },
    },
  ];

  return (
    <PageShell title="平台公告">
      {can("platform.announcement.create") ? (
        <div style={{ marginBottom: 12 }}>
          <Button type="primary" onClick={() => { form.resetFields(); setCreateOpen(true); }}>
            新建草稿
          </Button>
        </div>
      ) : null}
      <Table rowKey="announcementId" loading={loading} columns={columns} dataSource={items} pagination={false} />
      <Modal
        title="新建公告草稿"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={async () => {
          try {
            const v = await form.validateFields();
            await createAnnouncement(v);
            message.success("已创建草稿");
            setCreateOpen(false);
            load();
          } catch (e) {
            if (e?.errorFields) return;
            message.error(String(e?.response?.data?.message || e?.message || "失败"));
          }
        }}
        destroyOnClose
        width={560}
      >
        <Form form={form} layout="vertical" initialValues={{ target: "ALL_TENANTS" }}>
          <Form.Item name="title" label="标题" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="content" label="正文" rules={[{ required: true }]}>
            <Input.TextArea rows={5} />
          </Form.Item>
          <Form.Item name="target" label="范围" rules={[{ required: true }]}>
            <Select
              options={[
                { value: "ALL_TENANTS", label: "ALL_TENANTS" },
                { value: "TENANTS", label: "TENANTS" },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
      <Modal title="撤回公告" open={revokeOpen} onCancel={() => setRevokeOpen(false)} onOk={async () => {
        try {
          const v = await revokeForm.validateFields();
          await revokeAnnouncement(revokeId, v.reason);
          message.success("已撤回");
          setRevokeOpen(false);
          load();
        } catch (e) {
          if (e?.errorFields) return;
          message.error(String(e?.response?.data?.message || e?.message || "失败"));
        }
      }}
      >
        <Form form={revokeForm} layout="vertical">
          <Form.Item name="reason" label="原因" rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
      <Modal title="公告详情" open={detailOpen} onCancel={() => setDetailOpen(false)} footer={null} width={640}>
        {detail ? <pre style={{ fontSize: 12 }}>{JSON.stringify(detail, null, 2)}</pre> : null}
      </Modal>
    </PageShell>
  );
}

export default PlatformAnnouncementPage;
