import { useCallback, useEffect, useState } from "react";
import { Button, Dropdown, Form, Input, Modal, Select, Space, Table, Tag, message } from "antd";
import useAuthStore from "../../store/auth";
import { platformHasPerm } from "../../utils/platform-perm";
import {
  assignPlatformAccountRoles,
  createPlatformAccount,
  disablePlatformAccount,
  enablePlatformAccount,
  getPlatformAccount,
  queryPlatformAccounts,
  resetPlatformAccountPassword,
  updatePlatformAccount,
} from "../../api/platformAccount";
import { queryPlatformRoles } from "../../api/platformRole";
import PageShell from "../../components/page/PageShell";

function PlatformAccountPage() {
  const user = useAuthStore((s) => s.user);
  const can = (c) => platformHasPerm(user, c);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [roleOptions, setRoleOptions] = useState([]);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [rolesOpen, setRolesOpen] = useState(false);
  const [activeId, setActiveId] = useState(null);
  const [form] = Form.useForm();
  const [editForm] = Form.useForm();
  const [rolesForm] = Form.useForm();

  const loadRoles = useCallback(async () => {
    const res = await queryPlatformRoles({ page: 1, pageSize: 200 });
    const list = res.data?.data?.items || [];
    setRoleOptions(
      list.map((r) => ({ value: String(r.roleId), label: `${r.name} (${r.roleId})` })),
    );
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await queryPlatformAccounts({ page: 1, pageSize: 200 });
      setItems(res.data?.data?.items || []);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    loadRoles();
  }, [load, loadRoles]);

  const openDetail = async (accountId) => {
    setActiveId(accountId);
    setDetailOpen(true);
    try {
      const res = await getPlatformAccount(accountId);
      setDetail(res.data?.data || null);
    } catch {
      setDetail(null);
    }
  };

  const openRoles = async (accountId) => {
    setActiveId(accountId);
    const res = await getPlatformAccount(accountId);
    const ids = (res.data?.data?.roleIds || []).map(String);
    rolesForm.setFieldsValue({ roleIds: ids });
    setRolesOpen(true);
  };

  const openEdit = (row) => {
    setActiveId(row.accountId);
    editForm.setFieldsValue({
      displayName: row.displayName,
      phone: row.phone || "",
      email: row.email || "",
    });
    setEditOpen(true);
  };

  const columns = [
    { title: "显示名", dataIndex: "displayName" },
    { title: "用户名", dataIndex: "username" },
    { title: "状态", dataIndex: "status", width: 100, render: (s) => <Tag>{s}</Tag> },
    {
      title: "操作",
      width: 220,
      render: (_, row) => {
        const moreItems = [];
        if (can("platform.account.password.reset")) {
          moreItems.push({ key: "resetPwd", label: "重置密码" });
        }
        if (can("platform.account.role.assign")) {
          moreItems.push({ key: "roles", label: "分配角色" });
        }
        const onMore = ({ key }) => {
          if (key === "resetPwd") {
            Modal.confirm({
              title: "重置为系统默认密码？",
              onOk: () => resetPlatformAccountPassword(row.accountId, {}).then(() => { message.success("已重置"); }),
            });
          }
          if (key === "roles") openRoles(row.accountId);
        };
        return (
          <Space size={0} wrap>
            {can("platform.account.read") ? (
              <Button type="link" size="small" onClick={() => openDetail(row.accountId)}>
                详情
              </Button>
            ) : null}
            {can("platform.account.update") ? (
              <Button type="link" size="small" onClick={() => openEdit(row)}>
                编辑
              </Button>
            ) : null}
            {row.status === "ENABLED" && can("platform.account.disable") ? (
              <Button
                type="link"
                size="small"
                danger
                onClick={() => {
                  Modal.confirm({
                    title: "禁用账号？",
                    onOk: () => disablePlatformAccount(row.accountId).then(() => { message.success("已禁用"); load(); }),
                  });
                }}
              >
                禁用
              </Button>
            ) : null}
            {row.status === "DISABLED" && can("platform.account.enable") ? (
              <Button
                type="link"
                size="small"
                onClick={() => enablePlatformAccount(row.accountId).then(() => { message.success("已启用"); load(); })}
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
    <PageShell title="平台账号">
      {can("platform.account.create") ? (
        <div style={{ marginBottom: 12 }}>
          <Button
            type="primary"
            onClick={() => {
              form.resetFields();
              setCreateOpen(true);
            }}
          >
            新建账号
          </Button>
        </div>
      ) : null}
      <Table rowKey="accountId" loading={loading} columns={columns} dataSource={items} pagination={false} />
      <Modal
        title="新建平台账号"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={async () => {
          try {
            const v = await form.validateFields();
            await createPlatformAccount(v);
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
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="displayName" label="显示名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="phone" label="手机">
            <Input />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="编辑账号"
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={async () => {
          try {
            const v = await editForm.validateFields();
            await updatePlatformAccount(activeId, v);
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
          <Form.Item name="displayName" label="显示名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="phone" label="手机">
            <Input />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="分配平台角色"
        open={rolesOpen}
        onCancel={() => setRolesOpen(false)}
        onOk={async () => {
          try {
            const v = await rolesForm.validateFields();
            const roleIds = (v.roleIds || []).map((x) => Number(x));
            await assignPlatformAccountRoles(activeId, { roleIds });
            message.success("已保存");
            setRolesOpen(false);
            load();
          } catch (e) {
            if (e?.errorFields) return;
            message.error(String(e?.response?.data?.message || e?.message || "失败"));
          }
        }}
        destroyOnClose
        width={520}
      >
        <Form form={rolesForm} layout="vertical">
          <Form.Item name="roleIds" label="角色">
            <Select mode="multiple" options={roleOptions} optionFilterProp="label" />
          </Form.Item>
        </Form>
      </Modal>
      <Modal title="账号详情" open={detailOpen} onCancel={() => setDetailOpen(false)} footer={null} width={520}>
        {detail ? (
          <pre style={{ whiteSpace: "pre-wrap", fontSize: 12 }}>{JSON.stringify(detail, null, 2)}</pre>
        ) : null}
      </Modal>
    </PageShell>
  );
}

export default PlatformAccountPage;
