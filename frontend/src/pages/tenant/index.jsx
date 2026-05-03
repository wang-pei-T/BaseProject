import { useCallback, useEffect, useState } from "react";
import {
  Button,
  Descriptions,
  Drawer,
  Dropdown,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  message,
} from "antd";
import useAuthStore from "../../store/auth";
import { platformHasPerm } from "../../utils/platform-perm";
import {
  createPlatformTenant,
  deletePlatformTenant,
  disablePlatformTenant,
  enablePlatformTenant,
  forceLogoutTenantAdmin,
  getPlatformTenant,
  queryPlatformTenants,
  renewPlatformTenant,
  resetTenantAdminPassword,
  restorePlatformTenant,
  updatePlatformTenant,
} from "../../api/platformTenant";
import PageShell from "../../components/page/PageShell";
import QueryBar from "../../components/page/QueryBar";

function TenantListPage() {
  const user = useAuthStore((s) => s.user);
  const can = (c) => platformHasPerm(user, c);
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);
  const [loading, setLoading] = useState(false);
  const [kwDraft, setKwDraft] = useState("");
  const [kw, setKw] = useState("");
  const [status, setStatus] = useState("");
  const [includeDeleted, setIncludeDeleted] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [renewOpen, setRenewOpen] = useState(false);
  const [activeId, setActiveId] = useState(null);
  const [form] = Form.useForm();
  const [editForm] = Form.useForm();
  const [renewForm] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await queryPlatformTenants({
        page,
        pageSize,
        keyword: kw || undefined,
        status: status || undefined,
        includeDeleted,
      });
      const data = res.data?.data || {};
      setItems(data.items || []);
      setTotal(Number(data.total) || 0);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, kw, status, includeDeleted]);

  useEffect(() => {
    load();
  }, [load]);

  const openDetail = async (tenantId) => {
    setActiveId(tenantId);
    setDetailOpen(true);
    try {
      const res = await getPlatformTenant(tenantId, { includeDeleted });
      setDetail(res.data?.data || null);
    } catch {
      setDetail(null);
    }
  };

  const openEdit = (row) => {
    setActiveId(row.tenantId);
    editForm.setFieldsValue({
      tenantName: row.tenantName,
      expireAt: row.expireAt || "",
    });
    setEditOpen(true);
  };

  const openRenew = (row) => {
    setActiveId(row.tenantId);
    renewForm.setFieldsValue({ newExpireAt: row.expireAt || "" });
    setRenewOpen(true);
  };

  const columns = [
    { title: "租户名", dataIndex: "tenantName", width: 160 },
    { title: "编码", dataIndex: "tenantCode", width: 140 },
    { title: "状态", dataIndex: "status", width: 100 },
    { title: "到期", dataIndex: "expireAt", width: 140 },
    { title: "删除时间", dataIndex: "deletedAt", width: 180, render: (v) => v || "—" },
    {
      title: "操作",
      width: 260,
      fixed: "right",
      render: (_, row) => {
        const tid = row.tenantId;
        const deleted = !!row.deletedAt;
        const moreItems = [];
        if (!deleted && can("platform.tenant.renew")) {
          moreItems.push({ key: "renew", label: "续期" });
        }
        if (!deleted && can("platform.tenant.admin.reset_password")) {
          moreItems.push({ key: "resetPwd", label: "重置管理员密码" });
        }
        if (!deleted && can("platform.tenant.admin.force_logout")) {
          moreItems.push({ key: "forceLogout", label: "强退管理员" });
        }
        if (!deleted && can("platform.tenant.delete")) {
          moreItems.push({ key: "delete", label: "删除", danger: true });
        }
        if (deleted && can("platform.tenant.restore")) {
          moreItems.push({ key: "restore", label: "恢复" });
        }
        const onMore = ({ key }) => {
          if (key === "renew") openRenew(row);
          if (key === "resetPwd") {
            Modal.confirm({
              title: "重置租户管理员密码为系统默认？",
              onOk: () => resetTenantAdminPassword(tid, {}).then(() => { message.success("已重置"); load(); }),
            });
          }
          if (key === "forceLogout") {
            Modal.confirm({
              title: "强制下线租户管理员所有会话？",
              onOk: () => forceLogoutTenantAdmin(tid, {}).then(() => { message.success("已下线"); load(); }),
            });
          }
          if (key === "delete") {
            Modal.confirm({
              title: "软删除租户？",
              onOk: () => deletePlatformTenant(tid).then(() => { message.success("已删除"); load(); }),
            });
          }
          if (key === "restore") {
            Modal.confirm({
              title: "恢复租户？",
              onOk: () => restorePlatformTenant(tid).then(() => { message.success("已恢复"); load(); }),
            });
          }
        };
        return (
          <Space size={0} wrap>
            {can("platform.tenant.read") ? (
              <Button type="link" size="small" onClick={() => openDetail(tid)}>
                详情
              </Button>
            ) : null}
            {!deleted && can("platform.tenant.update") ? (
              <Button type="link" size="small" onClick={() => openEdit(row)}>
                编辑
              </Button>
            ) : null}
            {!deleted && row.status !== "ENABLED" && can("platform.tenant.enable") ? (
              <Button
                type="link"
                size="small"
                onClick={() => enablePlatformTenant(tid).then(() => { message.success("已启用"); load(); })}
              >
                启用
              </Button>
            ) : null}
            {!deleted && row.status === "ENABLED" && can("platform.tenant.disable") ? (
              <Button
                type="link"
                size="small"
                danger
                onClick={() => {
                  Modal.confirm({
                    title: "禁用租户？",
                    onOk: () => disablePlatformTenant(tid).then(() => { message.success("已禁用"); load(); }),
                  });
                }}
              >
                禁用
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
    <PageShell title="租户管理">
      <QueryBar
        loading={loading}
        onSearch={() => {
          setKw(kwDraft);
          setPage(1);
        }}
        onReset={() => {
          setKwDraft("");
          setKw("");
          setStatus("");
          setIncludeDeleted(false);
          setPage(1);
        }}
      >
        <Form.Item label="关键词">
          <Input value={kwDraft} onChange={(e) => setKwDraft(e.target.value)} placeholder="名称/编码" allowClear style={{ width: 200 }} />
        </Form.Item>
        <Form.Item label="状态">
          <Select
            value={status || undefined}
            onChange={(v) => setStatus(v || "")}
            allowClear
            placeholder="全部"
            style={{ width: 120 }}
            options={[
              { value: "ENABLED", label: "启用" },
              { value: "DISABLED", label: "停用" },
            ]}
          />
        </Form.Item>
        <Form.Item label="含已删">
          <Switch checked={includeDeleted} onChange={setIncludeDeleted} />
        </Form.Item>
      </QueryBar>
      {can("platform.tenant.create") ? (
        <div style={{ marginBottom: 12 }}>
          <Button type="primary" onClick={() => { form.resetFields(); setCreateOpen(true); }}>
            新建租户
          </Button>
        </div>
      ) : null}
      <Table
        rowKey="tenantId"
        loading={loading}
        columns={columns}
        dataSource={items}
        scroll={{ x: 1100 }}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: false,
          onChange: (p) => setPage(p),
        }}
      />
      <Drawer title="租户详情" open={detailOpen} onClose={() => setDetailOpen(false)} width={480}>
        {detail ? (
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="ID">{detail.tenantId}</Descriptions.Item>
            <Descriptions.Item label="编码">{detail.tenantCode}</Descriptions.Item>
            <Descriptions.Item label="名称">{detail.tenantName}</Descriptions.Item>
            <Descriptions.Item label="状态">{detail.status}</Descriptions.Item>
            <Descriptions.Item label="到期">{detail.expireAt || "—"}</Descriptions.Item>
            <Descriptions.Item label="管理员用户">{detail.adminUserId || "—"}</Descriptions.Item>
            <Descriptions.Item label="创建">{detail.createdAt}</Descriptions.Item>
            <Descriptions.Item label="更新">{detail.updatedAt}</Descriptions.Item>
            <Descriptions.Item label="删除">{detail.deletedAt || "—"}</Descriptions.Item>
          </Descriptions>
        ) : null}
      </Drawer>
      <Modal
        title="新建租户"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={async () => {
          try {
            const v = await form.validateFields();
            await createPlatformTenant(v);
            message.success("已创建");
            setCreateOpen(false);
            load();
          } catch (e) {
            if (e?.errorFields) return;
            message.error(String(e?.response?.data?.message || e?.message || "失败"));
          }
        }}
        destroyOnClose
        width={520}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="tenantCode" label="租户编码" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="tenantName" label="租户名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="expireAt" label="到期时间(可选 ISO 日期字符串)">
            <Input placeholder="如 2099-12-31" />
          </Form.Item>
          <Form.Item name="adminUsername" label="管理员登录名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="adminDisplayName" label="管理员显示名">
            <Input />
          </Form.Item>
          <Form.Item name="adminEmail" label="管理员邮箱">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="编辑租户"
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={async () => {
          try {
            const v = await editForm.validateFields();
            await updatePlatformTenant(activeId, v);
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
        <Form form={editForm} layout="vertical">
          <Form.Item name="tenantName" label="租户名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="expireAt" label="到期时间">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="续期"
        open={renewOpen}
        onCancel={() => setRenewOpen(false)}
        onOk={async () => {
          try {
            const v = await renewForm.validateFields();
            await renewPlatformTenant(activeId, { newExpireAt: v.newExpireAt });
            message.success("已续期");
            setRenewOpen(false);
            load();
          } catch (e) {
            if (e?.errorFields) return;
            message.error(String(e?.response?.data?.message || e?.message || "失败"));
          }
        }}
        destroyOnClose
      >
        <Form form={renewForm} layout="vertical">
          <Form.Item name="newExpireAt" label="新到期时间" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </PageShell>
  );
}

export default TenantListPage;
