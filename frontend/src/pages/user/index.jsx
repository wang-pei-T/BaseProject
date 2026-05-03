import { useCallback, useEffect, useMemo, useState } from "react";
import { QuestionCircleOutlined } from "@ant-design/icons";
import {
  Button,
  Descriptions,
  Drawer,
  Dropdown,
  Form,
  Input,
  Modal,
  Select,
  TreeSelect,
  Space,
  Tag,
  Tooltip,
  message,
  theme,
} from "antd";
import {
  createUser,
  deleteUser,
  disableUser,
  enableUser,
  getUser,
  lockUser,
  queryUsers,
  replaceUserRoles,
  resetUserPassword,
  restoreUser,
  unlockUser,
  updateUser,
} from "../../api/user";
import { queryOrgTree } from "../../api/org";
import { queryRoles } from "../../api/role";
import PageShell from "../../components/page/PageShell";
import QueryBar from "../../components/page/QueryBar";
import TableCard from "../../components/page/TableCard";
import { exportSheet } from "../../utils/exportExcel";
import { formatTs, renderStatusTag, statusLabelText } from "../../utils/tableDisplay";
import useRuntimeConfigStore from "../../store/runtime-config";

const emptyDraft = () => ({
  keyword: "",
  status: "",
  orgId: "",
  includeDeleted: "0",
});

function flattenOrgs(nodes, depth = 0) {
  const out = [];
  (nodes || []).forEach((n) => {
    const pad = "\u3000".repeat(depth);
    out.push({ value: String(n.orgId), label: `${pad}${n.name || n.orgId}` });
    if (n.children?.length) out.push(...flattenOrgs(n.children, depth + 1));
  });
  return out;
}

function buildOrgTreeFromFlat(flat) {
  const byId = new Map();
  (flat || []).forEach((o) => {
    const id = String(o.orgId);
    byId.set(id, { ...o, children: [] });
  });
  const roots = [];
  byId.forEach((node) => {
    const pid = node.parentOrgId != null ? String(node.parentOrgId) : "";
    if (pid && byId.has(pid)) {
      byId.get(pid).children.push(node);
    } else {
      roots.push(node);
    }
  });
  const sortRec = (nodes) => {
    nodes.sort((a, b) => (Number(a.sort) || 0) - (Number(b.sort) || 0));
    nodes.forEach((n) => {
      if (n.children?.length) sortRec(n.children);
    });
  };
  sortRec(roots);
  const prune = (nodes) => {
    nodes.forEach((n) => {
      if (n.children?.length) {
        prune(n.children);
      } else {
        delete n.children;
      }
    });
  };
  prune(roots);
  return roots;
}

function orgTreeToTreeSelectData(nodes) {
  return (nodes || []).map((n) => ({
    value: String(n.orgId),
    title: n.name || String(n.orgId),
    children: n.children?.length ? orgTreeToTreeSelectData(n.children) : undefined,
  }));
}

function collectOrgTitlesFromTree(nodes, map = new Map()) {
  (nodes || []).forEach((n) => {
    map.set(String(n.orgId), (n.name || String(n.orgId)).replace(/\u3000/g, " ").trim());
    if (n.children?.length) collectOrgTitlesFromTree(n.children, map);
  });
  return map;
}

function errMsg(e) {
  return e?.response?.data?.message || e?.message || "操作失败";
}

const phoneRules = [
  {
    validator: (_, v) => {
      const s = v == null ? "" : String(v).trim();
      if (!s) return Promise.resolve();
      if (!/^1[3-9]\d{9}$/.test(s)) return Promise.reject(new Error("请输入11位大陆手机号"));
      return Promise.resolve();
    },
  },
];

const emailRules = [
  {
    validator: (_, v) => {
      const s = v == null ? "" : String(v).trim();
      if (!s) return Promise.resolve();
      if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(s)) {
        return Promise.reject(new Error("邮箱格式不正确"));
      }
      return Promise.resolve();
    },
  },
];

const STATUS_HELP =
  "控制账号是否允许登录与使用；禁用后一般无法登录。";
const LOCK_HELP =
  "安全控制（如多次输错密码）；锁定后通常无法登录，需管理员解锁；与「禁用」含义不同，可同时存在。";

function lockLabelText(lockStatus) {
  if (lockStatus === "LOCKED") return "已锁定";
  return "未锁定";
}

function renderLockTag(lockStatus) {
  if (lockStatus === "LOCKED") return <Tag color="error">已锁定</Tag>;
  return <Tag>未锁定</Tag>;
}

function columnTitleWithHelp(title, help, helpIconColor) {
  return (
    <Space size={4}>
      {title}
      <Tooltip title={help} trigger={["hover", "click"]}>
        <QuestionCircleOutlined style={{ color: helpIconColor, cursor: "help" }} />
      </Tooltip>
    </Space>
  );
}

function UserPage() {
  const { token } = theme.useToken();
  const runtimeValues = useRuntimeConfigStore((state) => state.values);
  const defaultPageSize = Number(runtimeValues["ui.page.defaultSize"] || 10);
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(defaultPageSize);
  const [loading, setLoading] = useState(false);
  const [draftQuery, setDraftQuery] = useState(emptyDraft);
  const [activeQuery, setActiveQuery] = useState(emptyDraft);
  const [orgOptions, setOrgOptions] = useState([]);
  const [orgTreeSelectData, setOrgTreeSelectData] = useState([]);
  const [orgTitleById, setOrgTitleById] = useState(() => new Map());
  const [roles, setRoles] = useState([]);
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm();
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailId, setDetailId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editUserId, setEditUserId] = useState(null);
  const [editForm] = Form.useForm();
  const [roleOpen, setRoleOpen] = useState(false);
  const [roleUserId, setRoleUserId] = useState(null);
  const [roleForm] = Form.useForm();

  useEffect(() => {
    if (defaultPageSize > 0 && pageSize !== defaultPageSize) {
      setPageSize(defaultPageSize);
      setPage(1);
    }
  }, [defaultPageSize]);

  const roleNameById = useMemo(() => {
    const m = new Map();
    roles.forEach((r) => m.set(String(r.roleId), r.name || r.code || String(r.roleId)));
    return m;
  }, [roles]);

  const orgLabel = useCallback(
    (orgId) => {
      if (!orgId) return "-";
      const t = orgTitleById.get(String(orgId));
      return t || orgId;
    },
    [orgTitleById],
  );

  const loadList = useCallback(async () => {
    setLoading(true);
    try {
      const res = await queryUsers({
        page,
        pageSize,
        keyword: activeQuery.keyword?.trim() || undefined,
        status: activeQuery.status || undefined,
        orgId: activeQuery.orgId || undefined,
        includeDeleted: activeQuery.includeDeleted === "1",
      });
      const d = res.data?.data || {};
      setItems(d.items || []);
      setTotal(Number(d.total) || 0);
    } catch (e) {
      message.error(errMsg(e));
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, activeQuery.keyword, activeQuery.status, activeQuery.orgId, activeQuery.includeDeleted]);

  useEffect(() => {
    loadList();
  }, [loadList]);

  useEffect(() => {
    (async () => {
      try {
        const [treeRes, roleRes] = await Promise.all([
          queryOrgTree({ includeDisabled: true, includeDeleted: false }),
          queryRoles({ includeDeleted: false }),
        ]);
        const tree = treeRes.data?.data?.items || [];
        setOrgOptions(flattenOrgs(buildOrgTreeFromFlat(tree)));
        const hierarchy = buildOrgTreeFromFlat(tree);
        setOrgTreeSelectData(orgTreeToTreeSelectData(hierarchy));
        setOrgTitleById(collectOrgTitlesFromTree(hierarchy));
        setRoles(roleRes.data?.data?.items || []);
      } catch (e) {
        message.error(errMsg(e));
      }
    })();
  }, []);

  useEffect(() => {
    if (!detailId || !detailOpen) return;
    let cancelled = false;
    (async () => {
      setDetailLoading(true);
      try {
        const res = await getUser(detailId, { includeDeleted: activeQuery.includeDeleted === "1" });
        if (!cancelled) setDetail(res.data?.data || null);
      } catch (e) {
        if (!cancelled) message.error(errMsg(e));
      } finally {
        if (!cancelled) setDetailLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [detailId, detailOpen, activeQuery.includeDeleted]);

  const onSearch = () => {
    setActiveQuery({ ...draftQuery });
    setPage(1);
  };

  const resetQuery = () => {
    const q = emptyDraft();
    setDraftQuery(q);
    setActiveQuery(q);
    setPage(1);
  };

  const formatRoles = (row) => {
    const ids = row.roleIds || row.roleIdList;
    const list = Array.isArray(ids) ? ids : [];
    if (!list.length) return "-";
    return list.map((id) => roleNameById.get(String(id)) || id).join("，");
  };

  const openEdit = async (userId) => {
    setEditUserId(userId);
    try {
      const res = await getUser(userId, { includeDeleted: false });
      const u = res.data?.data;
      editForm.setFieldsValue({
        displayName: u?.displayName,
        phone: u?.phone,
        email: u?.email,
        orgId: u?.orgId != null && u.orgId !== "" ? String(u.orgId) : undefined,
      });
      setEditOpen(true);
    } catch (e) {
      message.error(errMsg(e));
    }
  };

  const submitEdit = async () => {
    const v = await editForm.validateFields();
    await updateUser(editUserId, v);
    message.success("已保存");
    setEditOpen(false);
    setEditUserId(null);
    loadList();
  };

  const onCreateSubmit = async () => {
    const values = await createForm.validateFields();
    const payload = {
      username: values.username,
      displayName: values.displayName,
      status: values.status || "ENABLED",
      phone: values.phone || undefined,
      email: values.email || undefined,
      orgId: values.orgId || undefined,
    };
    await createUser(payload);
    createForm.resetFields();
    setCreateOpen(false);
    message.success("已创建");
    loadList();
  };

  const submitRoles = async () => {
    const { roleIds } = await roleForm.validateFields();
    await replaceUserRoles(roleUserId, roleIds || []);
    message.success("已更新角色");
    setRoleOpen(false);
    setRoleUserId(null);
    loadList();
  };

  const openRoleModal = (row) => {
    setRoleUserId(row.userId);
    roleForm.setFieldsValue({ roleIds: (row.roleIds || []).map(String) });
    setRoleOpen(true);
  };

  const handleExport = () => {
    const headers = ["序号", "账号", "姓名", "手机号", "邮箱", "所属机构", "角色", "账号状态", "登录锁定", "删除时间"];
    const rows = items.map((r, i) => [
      (page - 1) * pageSize + i + 1,
      r.username || "",
      r.displayName || "",
      r.phone || "",
      r.email || "",
      orgLabel(r.orgId),
      formatRoles(r).replace(/，/g, ","),
      statusLabelText(r.status),
      lockLabelText(r.lockStatus),
      r.deletedAt != null ? String(r.deletedAt) : "",
    ]);
    exportSheet("用户列表", "用户", headers, rows);
    message.success("已导出");
  };

  const columns = [
    { title: "序号", width: 64, render: (_, __, i) => (page - 1) * pageSize + i + 1 },
    { title: "账号", dataIndex: "username", render: (t) => t || "-" },
    { title: "姓名", dataIndex: "displayName", render: (t) => t || "-" },
    { title: "手机号", dataIndex: "phone", render: (t) => t || "-" },
    { title: "邮箱", dataIndex: "email", render: (t) => t || "-" },
    { title: "所属机构", render: (_, r) => orgLabel(r.orgId) },
    { title: "角色", width: 160, ellipsis: true, render: (_, r) => formatRoles(r) },
    {
      title: columnTitleWithHelp("账号状态", STATUS_HELP, token.colorTextDescription),
      dataIndex: "status",
      width: 108,
      render: (s) => renderStatusTag(s),
    },
    {
      title: columnTitleWithHelp("登录锁定", LOCK_HELP, token.colorTextDescription),
      dataIndex: "lockStatus",
      width: 108,
      render: (s) => renderLockTag(s),
    },
    {
      title: "删除",
      width: 72,
      render: (_, r) => (r.deletedAt != null ? <Tag color="default">已删</Tag> : "—"),
    },
    {
      title: "操作",
      width: 280,
      fixed: "right",
      render: (_, row) => {
        const deleted = row.deletedAt != null;
        if (deleted) {
          return (
            <Space size={0} wrap>
              <Button type="link" size="small" onClick={() => { setDetailId(row.userId); setDetail(null); setDetailOpen(true); }}>
                详情
              </Button>
              <Dropdown
                menu={{
                  items: [{ key: "restore", label: "恢复" }],
                  onClick: ({ key }) => {
                    if (key === "restore") {
                      Modal.confirm({
                        title: "确认恢复该用户？",
                        onOk: () => restoreUser(row.userId).then(() => { message.success("已恢复"); loadList(); }),
                      });
                    }
                  },
                }}
                trigger={["click"]}
              >
                <Button type="link" size="small">
                  更多
                </Button>
              </Dropdown>
            </Space>
          );
        }
        const moreItems = [
          {
            key: "lock",
            label: row.lockStatus === "LOCKED" ? "解锁" : "锁定",
            onClick: () => {
              if (row.lockStatus === "LOCKED") {
                Modal.confirm({
                  title: "确认解锁？",
                  onOk: () => unlockUser(row.userId).then(() => { message.success("已解锁"); loadList(); }).catch((e) => message.error(errMsg(e))),
                });
              } else {
                Modal.confirm({
                  title: "确认锁定？",
                  onOk: () => lockUser(row.userId, {}).then(() => { message.success("已锁定"); loadList(); }).catch((e) => message.error(errMsg(e))),
                });
              }
            },
          },
          {
            key: "pwd",
            label: "重置密码",
            onClick: () => {
              Modal.confirm({
                title: "重置密码",
                content: "将重置为系统默认密码，用户需用新密码登录。",
                onOk: () => resetUserPassword(row.userId, {}).then(() => { message.success("已重置"); loadList(); }).catch((e) => message.error(errMsg(e))),
              });
            },
          },
          {
            key: "roles",
            label: "分配角色",
            onClick: () => openRoleModal(row),
          },
          {
            key: "del",
            label: "删除",
            danger: true,
            onClick: () => {
              Modal.confirm({
                title: "确认删除用户（软删）？",
                onOk: () => deleteUser(row.userId).then(() => { message.success("已删除"); loadList(); }).catch((e) => message.error(errMsg(e))),
              });
            },
          },
        ];
        return (
          <Space size={0} wrap>
            <Button type="link" size="small" onClick={() => { setDetailId(row.userId); setDetail(null); setDetailOpen(true); }}>
              详情
            </Button>
            <Button type="link" size="small" onClick={() => openEdit(row.userId)}>
              编辑
            </Button>
            {row.status === "DISABLED" ? (
              <Button type="link" size="small" onClick={() => enableUser(row.userId).then(() => { message.success("已启用"); loadList(); }).catch((e) => message.error(errMsg(e)))}>
                启用
              </Button>
            ) : (
              <Button
                type="link"
                size="small"
                danger
                onClick={() => {
                  Modal.confirm({
                    title: "确认禁用？",
                    onOk: () => disableUser(row.userId).then(() => { message.success("已禁用"); loadList(); }).catch((e) => message.error(errMsg(e))),
                  });
                }}
              >
                禁用
              </Button>
            )}
            <Dropdown menu={{ items: moreItems }} trigger={["click"]}>
              <Button type="link" size="small">
                更多
              </Button>
            </Dropdown>
          </Space>
        );
      },
    },
  ];

  return (
    <PageShell title="用户管理">
      <QueryBar
        onSearch={onSearch}
        onReset={resetQuery}
        loading={loading}
      >
        <Form.Item label="关键词">
          <Input
            allowClear
            placeholder="账号/姓名/手机/邮箱"
            value={draftQuery.keyword}
            onChange={(e) => setDraftQuery((p) => ({ ...p, keyword: e.target.value }))}
            style={{ width: 200 }}
          />
        </Form.Item>
        <Form.Item label="状态">
          <Select
            allowClear
            placeholder="全部"
            style={{ width: 120 }}
            value={draftQuery.status || undefined}
            onChange={(v) => setDraftQuery((p) => ({ ...p, status: v || "" }))}
            options={[
              { value: "ENABLED", label: "启用" },
              { value: "DISABLED", label: "禁用" },
            ]}
          />
        </Form.Item>
        <Form.Item label="所属机构">
          <Select
            allowClear
            showSearch
            optionFilterProp="label"
            placeholder="全部"
            style={{ width: 200 }}
            value={draftQuery.orgId || undefined}
            onChange={(v) => setDraftQuery((p) => ({ ...p, orgId: v || "" }))}
            options={orgOptions}
          />
        </Form.Item>
        <Form.Item label="含已删除">
          <Select
            style={{ width: 100 }}
            value={draftQuery.includeDeleted}
            onChange={(v) => setDraftQuery((p) => ({ ...p, includeDeleted: v }))}
            options={[
              { value: "0", label: "否" },
              { value: "1", label: "是" },
            ]}
          />
        </Form.Item>
      </QueryBar>
      <TableCard
        title="用户列表"
        rowKey={(r) => r.userId}
        loading={loading}
        columns={columns}
        dataSource={items}
        scroll={{ x: 1200 }}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p, ps) => {
            setPage(p);
            setPageSize(ps);
          },
        }}
        extra={
          <Space wrap>
            <Button
              type="primary"
              onClick={() => {
                createForm.setFieldsValue({ status: "ENABLED" });
                setCreateOpen(true);
              }}
            >
              新增用户
            </Button>
            <Button onClick={handleExport}>导出 Excel</Button>
          </Space>
        }
      />

      <Modal title="新增用户" open={createOpen} onCancel={() => { setCreateOpen(false); createForm.resetFields(); }} onOk={onCreateSubmit} destroyOnClose width={520}>
        <Form form={createForm} layout="vertical" initialValues={{ status: "ENABLED" }}>
          <Form.Item name="username" label="账号" rules={[{ required: true, message: "请输入账号" }]}>
            <Input placeholder="登录账号" />
          </Form.Item>
          <Form.Item name="displayName" label="姓名" rules={[{ required: true, message: "请输入姓名" }]}>
            <Input placeholder="显示名" />
          </Form.Item>
          <Form.Item name="phone" label="手机" rules={phoneRules}>
            <Input placeholder="可选" />
          </Form.Item>
          <Form.Item name="email" label="邮箱" rules={emailRules}>
            <Input placeholder="可选" />
          </Form.Item>
          <Form.Item name="orgId" label="所属机构">
            <TreeSelect
              allowClear
              showSearch
              treeDefaultExpandAll
              placeholder="可选"
              treeData={orgTreeSelectData}
              treeNodeFilterProp="title"
              style={{ width: "100%" }}
            />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ value: "ENABLED", label: "启用" }, { value: "DISABLED", label: "禁用" }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="编辑用户" open={editOpen} onCancel={() => { setEditOpen(false); setEditUserId(null); editForm.resetFields(); }} onOk={submitEdit} destroyOnClose width={520}>
        <Form form={editForm} layout="vertical">
          <Form.Item name="displayName" label="姓名" rules={[{ required: true, message: "请输入姓名" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="phone" label="手机" rules={phoneRules}>
            <Input allowClear />
          </Form.Item>
          <Form.Item name="email" label="邮箱" rules={emailRules}>
            <Input allowClear />
          </Form.Item>
          <Form.Item name="orgId" label="所属机构">
            <TreeSelect
              allowClear
              showSearch
              treeDefaultExpandAll
              placeholder="可选"
              treeData={orgTreeSelectData}
              treeNodeFilterProp="title"
              style={{ width: "100%" }}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="分配角色" open={roleOpen} onCancel={() => { setRoleOpen(false); setRoleUserId(null); roleForm.resetFields(); }} onOk={submitRoles} destroyOnClose width={520}>
        <Form form={roleForm} layout="vertical">
          <Form.Item name="roleIds" label="角色（全量替换）">
            <Select mode="multiple" allowClear placeholder="选择角色" options={roles.map((r) => ({ value: String(r.roleId), label: r.name || r.code }))} optionFilterProp="label" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="用户详情"
        width={480}
        open={detailOpen}
        onClose={() => { setDetailOpen(false); setDetailId(null); setDetail(null); }}
        destroyOnHidden
      >
        {detailLoading ? (
          <div>加载中…</div>
        ) : detail ? (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="用户ID">{detail.userId}</Descriptions.Item>
            <Descriptions.Item label="账号">{detail.username}</Descriptions.Item>
            <Descriptions.Item label="姓名">{detail.displayName}</Descriptions.Item>
            <Descriptions.Item label="手机">{detail.phone || "—"}</Descriptions.Item>
            <Descriptions.Item label="邮箱">{detail.email || "—"}</Descriptions.Item>
            <Descriptions.Item label="机构">{orgLabel(detail.orgId)}</Descriptions.Item>
            <Descriptions.Item label={columnTitleWithHelp("账号状态", STATUS_HELP, token.colorTextDescription)}>
              {renderStatusTag(detail.status)}
            </Descriptions.Item>
            <Descriptions.Item label={columnTitleWithHelp("登录锁定", LOCK_HELP, token.colorTextDescription)}>
              {renderLockTag(detail.lockStatus)}
            </Descriptions.Item>
            <Descriptions.Item label="角色">{formatRoles(detail)}</Descriptions.Item>
            <Descriptions.Item label="删除时间">{formatTs(detail.deletedAt)}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatTs(detail.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{formatTs(detail.updatedAt)}</Descriptions.Item>
          </Descriptions>
        ) : null}
      </Drawer>
    </PageShell>
  );
}

export default UserPage;
