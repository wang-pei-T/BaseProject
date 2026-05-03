import { useCallback, useEffect, useState } from "react";
import { Button, Card, Checkbox, Dropdown, Form, Input, Modal, Space, Table, Tag, message } from "antd";
import useAuthStore from "../../store/auth";
import { platformHasPerm } from "../../utils/platform-perm";
import {
  createPlatformRole,
  deletePlatformRole,
  disablePlatformRole,
  enablePlatformRole,
  getPlatformRole,
  queryPlatformPermissions,
  queryPlatformRoles,
  replacePlatformRolePermissions,
  updatePlatformRole,
} from "../../api/platformRole";
import PageShell from "../../components/page/PageShell";

function PlatformRolePage() {
  const user = useAuthStore((s) => s.user);
  const can = (c) => platformHasPerm(user, c);
  const [roles, setRoles] = useState([]);
  const [perms, setPerms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [permOpen, setPermOpen] = useState(false);
  const [permLoading, setPermLoading] = useState(false);
  const [selectedPermCodes, setSelectedPermCodes] = useState([]);
  const [activeRoleId, setActiveRoleId] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailJson, setDetailJson] = useState(null);
  const [form] = Form.useForm();
  const [editForm] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [r, p] = await Promise.all([
        queryPlatformRoles({ page: 1, pageSize: 200 }),
        queryPlatformPermissions({ page: 1, pageSize: 500 }),
      ]);
      setRoles(r.data?.data?.items || []);
      setPerms(p.data?.data?.items || []);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openPerms = async (roleId) => {
    setActiveRoleId(roleId);
    setPermOpen(true);
    setPermLoading(true);
    try {
      const res = await getPlatformRole(roleId);
      const list = res.data?.data?.permissions || [];
      setSelectedPermCodes(list);
    } finally {
      setPermLoading(false);
    }
  };

  const openEdit = (row) => {
    setActiveRoleId(row.roleId);
    editForm.setFieldsValue({ name: row.name, code: row.code });
    setEditOpen(true);
  };

  const openRoleDetail = async (roleId) => {
    setDetailOpen(true);
    setDetailJson(null);
    try {
      const res = await getPlatformRole(roleId);
      setDetailJson(res.data?.data || null);
    } catch {
      setDetailJson(null);
    }
  };

  const permOptions = perms.map((p) => ({
    label: `${p.permissionCode}${p.description ? ` — ${p.description}` : ""}`,
    value: p.permissionCode,
  }));

  const columns = [
    { title: "编码", dataIndex: "code", width: 160 },
    { title: "角色名", dataIndex: "name" },
    { title: "状态", dataIndex: "status", width: 100, render: (s) => <Tag>{s}</Tag> },
    {
      title: "操作",
      width: 260,
      render: (_, row) => {
        const moreItems = [];
        if (can("platform.role.permission.assign")) moreItems.push({ key: "perm", label: "权限" });
        if (can("platform.role.delete")) moreItems.push({ key: "del", label: "删除", danger: true });
        const onMore = ({ key }) => {
          if (key === "perm") openPerms(row.roleId);
          if (key === "del") {
            Modal.confirm({
              title: "软删除角色？",
              onOk: () => deletePlatformRole(row.roleId).then(() => { message.success("已删除"); load(); }),
            });
          }
        };
        return (
          <Space size={0} wrap>
            {can("platform.role.read") ? (
              <Button type="link" size="small" onClick={() => openRoleDetail(row.roleId)}>
                详情
              </Button>
            ) : null}
            {can("platform.role.update") ? (
              <Button type="link" size="small" onClick={() => openEdit(row)}>
                编辑
              </Button>
            ) : null}
            {row.status === "ENABLED" && can("platform.role.disable") ? (
              <Button
                type="link"
                size="small"
                danger
                onClick={() => {
                  Modal.confirm({
                    title: "禁用角色？",
                    onOk: () => disablePlatformRole(row.roleId).then(() => { message.success("已禁用"); load(); }),
                  });
                }}
              >
                禁用
              </Button>
            ) : null}
            {row.status === "DISABLED" && can("platform.role.enable") ? (
              <Button
                type="link"
                size="small"
                onClick={() => enablePlatformRole(row.roleId).then(() => { message.success("已启用"); load(); })}
              >
                启用
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
    <PageShell title="平台角色">
      {can("platform.role.create") ? (
        <div style={{ marginBottom: 12 }}>
          <Button type="primary" onClick={() => { form.resetFields(); setCreateOpen(true); }}>
            新建角色
          </Button>
        </div>
      ) : null}
      <Card size="small" title="角色列表" loading={loading} style={{ marginBottom: 16 }}>
        <Table rowKey="roleId" size="small" columns={columns} dataSource={roles} pagination={false} />
      </Card>
      <Card size="small" title="平台权限点（只读）">
        <Table
          rowKey={(r, i) => `${r.permissionCode}-${i}`}
          size="small"
          columns={[
            { title: "权限点", dataIndex: "permissionCode" },
            { title: "说明", dataIndex: "description" },
          ]}
          dataSource={perms}
          pagination={false}
          scroll={{ y: 240 }}
        />
      </Card>
      <Modal
        title="新建角色"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={async () => {
          try {
            const v = await form.validateFields();
            await createPlatformRole(v);
            message.success("已创建");
            setCreateOpen(false);
            load();
          } catch (e) {
            if (e?.errorFields) return;
            message.error(String(e?.response?.data?.message || e?.message || "失败"));
          }
        }}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="角色名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="code" label="编码(可选)">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="编辑角色"
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={async () => {
          try {
            const v = await editForm.validateFields();
            await updatePlatformRole(activeRoleId, v);
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
          <Form.Item name="name" label="角色名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="code" label="编码">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
      <Modal title="角色详情" open={detailOpen} onCancel={() => setDetailOpen(false)} footer={null} width={640}>
        <pre style={{ fontSize: 12, maxHeight: 480, overflow: "auto" }}>{detailJson ? JSON.stringify(detailJson, null, 2) : ""}</pre>
      </Modal>
      <Modal
        title="替换角色权限"
        open={permOpen}
        onCancel={() => setPermOpen(false)}
        width={720}
        confirmLoading={permLoading}
        onOk={async () => {
          try {
            await replacePlatformRolePermissions(activeRoleId, { permissions: selectedPermCodes });
            message.success("已保存");
            setPermOpen(false);
            load();
          } catch (e) {
            message.error(String(e?.response?.data?.message || e?.message || "失败"));
          }
        }}
      >
        <div style={{ maxHeight: 420, overflow: "auto" }}>
          <Checkbox.Group
            style={{ width: "100%" }}
            value={selectedPermCodes}
            onChange={setSelectedPermCodes}
            options={permOptions}
          />
        </div>
      </Modal>
    </PageShell>
  );
}

export default PlatformRolePage;
