import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert, Button, Descriptions, Divider, Drawer, Dropdown, Form, Input, Modal, Segmented, Select, Space, Table, Tag, Tree, Typography, message, theme } from "antd";
import {
  createRole,
  deleteRole,
  disableRole,
  enableRole,
  getRole,
  queryRoleMenus,
  queryRoles,
  replaceRoleMenus,
  restoreRole,
  updateRole,
} from "../../api/role";
import { queryMenuTree } from "../../api/menu";
import PageShell from "../../components/page/PageShell";
import QueryBar from "../../components/page/QueryBar";
import TableCard from "../../components/page/TableCard";
import { exportSheet } from "../../utils/exportExcel";
import { formatTs, renderStatusTag, statusLabelText } from "../../utils/tableDisplay";
import useRuntimeConfigStore from "../../store/runtime-config";

const emptyDraft = () => ({
  keyword: "",
  status: "",
  roleType: "",
  includeDeleted: "0",
});

function errMsg(e) {
  return e?.response?.data?.message || e?.message || "操作失败";
}

function friendlyCode(msg) {
  const map = {
    ROLE_IN_USE: "角色已被用户引用，无法删除",
    ROLE_DISABLED: "角色已禁用，不能分配",
    ROLE_NOT_FOUND: "角色不存在",
    ROLE_NOT_DELETED: "角色未删除",
    MENU_NOT_FOUND: "包含不存在的菜单",
    CONFLICT_UNIQUE: "角色名称或编码已存在",
    FORBIDDEN: "无权限查看含已删除数据",
    VALIDATION_ERROR: "参数校验失败",
  };
  return map[msg] || msg;
}

function RolePage() {
  const { token } = theme.useToken();
  const runtimeValues = useRuntimeConfigStore((state) => state.values);
  const defaultPageSize = Number(runtimeValues["ui.page.defaultSize"] || 20);
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(defaultPageSize);

  const [draftQuery, setDraftQuery] = useState(emptyDraft);
  const [activeQuery, setActiveQuery] = useState(emptyDraft);

  const [menuLoading, setMenuLoading] = useState(false);
  const [grantMenuTree, setGrantMenuTree] = useState([]);
  const [grantCheckedKeys, setGrantCheckedKeys] = useState([]);

  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [grantOpen, setGrantOpen] = useState(false);
  const [currentRoleId, setCurrentRoleId] = useState(null);
  const [currentRole, setCurrentRole] = useState(null);
  const [grantRoleMeta, setGrantRoleMeta] = useState(null);

  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const [grantKeyword, setGrantKeyword] = useState("");
  const [grantExpandedKeys, setGrantExpandedKeys] = useState([]);
  const [grantInitialCheckedKeys, setGrantInitialCheckedKeys] = useState([]);
  const [grantView, setGrantView] = useState("tree");

  useEffect(() => {
    if (defaultPageSize > 0 && pageSize !== defaultPageSize) {
      setPageSize(defaultPageSize);
      setPage(1);
    }
  }, [defaultPageSize]);

  const buildMenuTree = useCallback((flat) => {
    const byId = new Map();
    (flat || []).forEach((m) => {
      const id = String(m.menuId);
      byId.set(id, {
        key: id,
        title: m.name || id,
        children: [],
      });
    });
    const roots = [];
    (flat || []).forEach((m) => {
      const id = String(m.menuId);
      const pid = m.parentId != null ? String(m.parentId) : "";
      const node = byId.get(id);
      if (pid && byId.has(pid)) {
        byId.get(pid).children.push(node);
      } else {
        roots.push(node);
      }
    });
    const prune = (nodes) => {
      nodes.forEach((n) => {
        if (n.children.length) {
          prune(n.children);
        } else {
          delete n.children;
        }
      });
    };
    prune(roots);
    return roots;
  }, []);

  const filterTreeByKeyword = useCallback((nodes, keyword) => {
    if (!keyword) return nodes;
    const k = keyword.toLowerCase();
    const walk = (items) => {
      const out = [];
      (items || []).forEach((n) => {
        const children = walk(n.children || []);
        const hit = String(n.title || "").toLowerCase().includes(k);
        if (hit || children.length) {
          out.push({ ...n, ...(children.length ? { children } : {}) });
        }
      });
      return out;
    };
    return walk(nodes);
  }, []);

  const collectKeys = useCallback((nodes, out = []) => {
    (nodes || []).forEach((n) => {
      out.push(String(n.key));
      if (n.children?.length) collectKeys(n.children, out);
    });
    return out;
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await queryRoles({
        page,
        pageSize,
        keyword: activeQuery.keyword || undefined,
        status: activeQuery.status || undefined,
        includeDeleted: activeQuery.includeDeleted === "1",
      });
      const d = res.data?.data || {};
      let list = d.items || [];
      if (activeQuery.roleType === "BUILTIN") list = list.filter((r) => !!r.builtin);
      if (activeQuery.roleType === "CUSTOM") list = list.filter((r) => !r.builtin);
      setRows(list);
      setTotal(Number(d.total) || 0);
    } catch (e) {
      message.error(friendlyCode(errMsg(e)));
      setRows([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, activeQuery]);

  useEffect(() => {
    load();
  }, [load]);

  const onSearch = () => {
    setPage(1);
    setActiveQuery({ ...draftQuery });
  };

  const resetQuery = () => {
    const q = emptyDraft();
    setPage(1);
    setDraftQuery(q);
    setActiveQuery(q);
  };

  const openDetail = async (roleId) => {
    setCurrentRoleId(roleId);
    try {
      const res = await getRole(roleId, { includeDeleted: activeQuery.includeDeleted === "1" });
      setCurrentRole(res.data?.data || null);
      setDetailOpen(true);
    } catch (e) {
      message.error(friendlyCode(errMsg(e)));
    }
  };

  const openEdit = async (roleId) => {
    setCurrentRoleId(roleId);
    try {
      const res = await getRole(roleId, { includeDeleted: false });
      const role = res.data?.data || {};
      editForm.setFieldsValue({
        name: role.name,
        code: role.code,
        description: role.description,
      });
      setEditOpen(true);
    } catch (e) {
      message.error(friendlyCode(errMsg(e)));
    }
  };

  const openGrant = async (role) => {
    setCurrentRoleId(role.roleId);
    setGrantRoleMeta(role);
    setGrantKeyword("");
    setGrantView("tree");
    setMenuLoading(true);
    try {
      const [menuRes, roleMenuRes] = await Promise.all([
        queryMenuTree({ includeDeleted: false, includeDisabled: true }),
        queryRoleMenus(role.roleId),
      ]);
      const rawItems = menuRes.data?.data?.items || [];
      const menuFlat = [];
      const flattenForTree = (arr) => {
        (arr || []).forEach((node) => {
          const { children, ...rest } = node;
          menuFlat.push(rest);
          if (children && children.length) flattenForTree(children);
        });
      };
      flattenForTree(rawItems);
      const roleMenuIds = roleMenuRes.data?.data?.menuIds || [];
      const tree = buildMenuTree(menuFlat);
      const checked = (roleMenuIds || []).map((id) => String(id));
      setGrantMenuTree(tree);
      setGrantCheckedKeys(checked);
      setGrantInitialCheckedKeys(checked);
      setGrantExpandedKeys(collectKeys(tree));
      setGrantOpen(true);
    } catch (e) {
      message.error(friendlyCode(errMsg(e)));
    } finally {
      setMenuLoading(false);
    }
  };

  const onCreateSubmit = async () => {
    try {
      const values = await createForm.validateFields();
      await createRole({
        name: values.name,
        code: values.code || undefined,
        description: values.description || undefined,
        status: values.status || "ENABLED",
      });
      createForm.resetFields();
      setCreateOpen(false);
      message.success("已创建");
      load();
    } catch (e) {
      if (e?.errorFields) throw e;
      message.error(friendlyCode(errMsg(e)));
      throw e;
    }
  };

  const onEditSubmit = async () => {
    try {
      const values = await editForm.validateFields();
      await updateRole(currentRoleId, {
        name: values.name,
        code: values.code || undefined,
        description: values.description || undefined,
      });
      setEditOpen(false);
      setCurrentRoleId(null);
      editForm.resetFields();
      message.success("已保存");
      load();
    } catch (e) {
      if (e?.errorFields) throw e;
      message.error(friendlyCode(errMsg(e)));
      throw e;
    }
  };

  const onGrantSubmit = async () => {
    try {
      const removed = grantInitialCheckedKeys.filter((k) => !grantCheckedKeys.includes(k));
      const doSubmit = async () => {
        await replaceRoleMenus(currentRoleId, grantCheckedKeys.map((id) => String(id)));
        setGrantOpen(false);
        setCurrentRoleId(null);
        setGrantMenuTree([]);
        setGrantCheckedKeys([]);
        setGrantInitialCheckedKeys([]);
        setGrantExpandedKeys([]);
        setGrantKeyword("");
        setGrantRoleMeta(null);
        message.success("菜单授权已保存");
        load();
      };
      if (removed.length > 0) {
        Modal.confirm({
          title: "确认保存本次授权变更？",
          content: `将移除 ${removed.length} 项菜单授权，保存后立即生效。`,
          onOk: () => doSubmit(),
        });
      } else {
        await doSubmit();
      }
    } catch (e) {
      if (e?.errorFields) throw e;
      message.error(friendlyCode(errMsg(e)));
      throw e;
    }
  };

  const handleExport = () => {
    const headers = ["序号", "角色名称", "角色编码", "类型", "状态", "用户数", "描述", "创建时间", "删除"];
    const data = rows.map((r, i) => [
      i + 1,
      r.name || "",
      r.code || "",
      r.builtin ? "内置" : "自定义",
      statusLabelText(r.status),
      r.userCount ?? "",
      r.description || "",
      formatTs(r.createdAt),
      r.deletedAt != null ? "已删" : "",
    ]);
    exportSheet("角色列表", "角色", headers, data);
    message.success("已导出");
  };

  const menuLabelById = useMemo(() => {
    const out = new Map();
    const walk = (nodes) => {
      (nodes || []).forEach((n) => {
        out.set(String(n.key), String(n.title || n.key));
        if (n.children?.length) walk(n.children);
      });
    };
    walk(grantMenuTree);
    return out;
  }, [grantMenuTree]);

  const filteredGrantTree = useMemo(
    () => filterTreeByKeyword(grantMenuTree, grantKeyword.trim()),
    [grantMenuTree, grantKeyword, filterTreeByKeyword],
  );
  const visibleKeys = useMemo(() => collectKeys(filteredGrantTree, []), [filteredGrantTree, collectKeys]);
  const addedMenuKeys = useMemo(
    () => grantCheckedKeys.filter((k) => !grantInitialCheckedKeys.includes(k)),
    [grantCheckedKeys, grantInitialCheckedKeys],
  );
  const removedMenuKeys = useMemo(
    () => grantInitialCheckedKeys.filter((k) => !grantCheckedKeys.includes(k)),
    [grantCheckedKeys, grantInitialCheckedKeys],
  );

  const columns = [
    { title: "角色名称", dataIndex: "name", width: 180, ellipsis: true, render: (v) => v || "-" },
    { title: "角色编码", dataIndex: "code", width: 150, ellipsis: true, render: (v) => v || "-" },
    { title: "类型", dataIndex: "builtin", width: 80, render: (v) => (v ? "内置" : "自定义") },
    { title: "状态", dataIndex: "status", width: 90, render: (s) => renderStatusTag(s) },
    { title: "用户数", dataIndex: "userCount", width: 80, render: (v) => v ?? "-" },
    { title: "描述", dataIndex: "description", width: 220, ellipsis: true, render: (v) => v || "-" },
    {
      title: "操作",
      width: 320,
      fixed: "right",
      render: (_, item) => {
        const deleted = item.deletedAt != null;
        if (deleted) {
          return (
            <Space size={0} wrap>
              <Button type="link" size="small" onClick={() => openDetail(item.roleId)}>
                详情
              </Button>
              <Dropdown
                menu={{
                  items: [{ key: "restore", label: "恢复" }],
                  onClick: ({ key }) => {
                    if (key === "restore") {
                      restoreRole(item.roleId)
                        .then(() => {
                          message.success("已恢复");
                          load();
                        })
                        .catch((e) => message.error(friendlyCode(errMsg(e))));
                    }
                  },
                }}
              >
                <Button type="link" size="small">
                  更多
                </Button>
              </Dropdown>
            </Space>
          );
        }
        return (
          <Space size={0} wrap>
            <Button type="link" size="small" onClick={() => openDetail(item.roleId)}>
              详情
            </Button>
            <Button type="link" size="small" onClick={() => openEdit(item.roleId)}>
              编辑
            </Button>
            {item.status === "DISABLED" ? (
              <Button
                type="link"
                size="small"
                onClick={() =>
                  enableRole(item.roleId)
                    .then(() => {
                      message.success("已启用");
                      load();
                    })
                    .catch((e) => message.error(friendlyCode(errMsg(e))))
                }
              >
                启用
              </Button>
            ) : (
              <Button
                type="link"
                size="small"
                danger
                onClick={() =>
                  disableRole(item.roleId)
                    .then(() => {
                      message.success("已禁用");
                      load();
                    })
                    .catch((e) => message.error(friendlyCode(errMsg(e))))
                }
              >
                禁用
              </Button>
            )}
            <Dropdown
              menu={{
                items: [
                  { key: "grant", label: "菜单授权" },
                  { type: "divider" },
                  { key: "delete", label: "删除", danger: true },
                ],
                onClick: ({ key }) => {
                  if (key === "grant") {
                    openGrant(item);
                  }
                  if (key === "delete") {
                    Modal.confirm({
                      title: "确认删除该角色？",
                      onOk: () =>
                        deleteRole(item.roleId)
                          .then(() => {
                            message.success("已删除");
                            load();
                          })
                          .catch((e) => message.error(friendlyCode(errMsg(e)))),
                    });
                  }
                },
              }}
            >
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
    <PageShell title="角色管理">
      <QueryBar onSearch={onSearch} onReset={resetQuery} loading={loading}>
        <Form.Item label="关键词">
          <Input
            allowClear
            placeholder="角色名称/编码"
            value={draftQuery.keyword}
            onChange={(e) => setDraftQuery((p) => ({ ...p, keyword: e.target.value }))}
            style={{ width: 220 }}
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
        <Form.Item label="类型">
          <Select
            allowClear
            placeholder="全部"
            style={{ width: 120 }}
            value={draftQuery.roleType || undefined}
            onChange={(v) => setDraftQuery((p) => ({ ...p, roleType: v || "" }))}
            options={[
              { value: "BUILTIN", label: "内置" },
              { value: "CUSTOM", label: "自定义" },
            ]}
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
        title="角色列表"
        rowKey={(r) => r.roleId}
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: false,
          onChange: (p) => setPage(p),
        }}
        scroll={{ x: 1220 }}
        extra={
          <Space wrap>
            <Button
              type="primary"
              onClick={() => {
                createForm.resetFields();
                createForm.setFieldsValue({ status: "ENABLED" });
                setCreateOpen(true);
              }}
            >
              新增角色
            </Button>
            <Button onClick={handleExport}>导出 Excel</Button>
          </Space>
        }
      />
      <Modal
        title="新增角色"
        open={createOpen}
        onCancel={() => {
          setCreateOpen(false);
          createForm.resetFields();
        }}
        onOk={onCreateSubmit}
        destroyOnClose
        width={520}
      >
        <Form form={createForm} layout="vertical" initialValues={{ status: "ENABLED" }}>
          <Form.Item name="name" label="角色名称" rules={[{ required: true, message: "请输入角色名称" }]}>
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item name="code" label="角色编码">
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} maxLength={200} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ value: "ENABLED", label: "启用" }, { value: "DISABLED", label: "禁用" }]} />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="编辑角色"
        open={editOpen}
        onCancel={() => {
          setEditOpen(false);
          setCurrentRoleId(null);
          editForm.resetFields();
        }}
        onOk={onEditSubmit}
        destroyOnClose
        width={520}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item name="name" label="角色名称" rules={[{ required: true, message: "请输入角色名称" }]}>
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item name="code" label="角色编码">
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="菜单授权"
        open={grantOpen}
        onCancel={() => {
          setGrantOpen(false);
          setCurrentRoleId(null);
          setGrantMenuTree([]);
          setGrantCheckedKeys([]);
          setGrantInitialCheckedKeys([]);
          setGrantExpandedKeys([]);
          setGrantKeyword("");
          setGrantRoleMeta(null);
        }}
        onOk={onGrantSubmit}
        okButtonProps={{ loading: menuLoading }}
        okText="保存授权"
        cancelText="取消"
        destroyOnClose
        width={980}
      >
        <Space direction="vertical" size={12} style={{ width: "100%" }}>
          <Descriptions bordered size="small" column={3}>
            <Descriptions.Item label="角色名称">{grantRoleMeta?.name || "—"}</Descriptions.Item>
            <Descriptions.Item label="角色编码">{grantRoleMeta?.code || "—"}</Descriptions.Item>
            <Descriptions.Item label="已选菜单数">{grantCheckedKeys.length}</Descriptions.Item>
          </Descriptions>
          <Space wrap style={{ width: "100%", justifyContent: "space-between" }}>
            <Input.Search
              allowClear
              value={grantKeyword}
              onChange={(e) => setGrantKeyword(e.target.value)}
              placeholder="搜索菜单名称"
              style={{ width: 260 }}
            />
            <Space wrap>
              <Segmented
                options={[
                  { value: "tree", label: "树视图" },
                  { value: "selected", label: "已选清单" },
                ]}
                value={grantView}
                onChange={(v) => setGrantView(v)}
              />
              <Button onClick={() => setGrantExpandedKeys(collectKeys(filteredGrantTree, []))}>展开全部</Button>
              <Button onClick={() => setGrantExpandedKeys([])}>收起全部</Button>
              <Button
                onClick={() => {
                  const next = new Set(grantCheckedKeys);
                  visibleKeys.forEach((k) => next.add(k));
                  setGrantCheckedKeys([...next]);
                }}
              >
                全选当前结果
              </Button>
              <Button
                onClick={() => {
                  const drop = new Set(visibleKeys);
                  setGrantCheckedKeys(grantCheckedKeys.filter((k) => !drop.has(k)));
                }}
              >
                清空当前结果
              </Button>
            </Space>
          </Space>
          <Alert type="info" showIcon message="保存后立即生效，影响拥有该角色的用户菜单可见性。" />
          <div style={{ display: "grid", gridTemplateColumns: "1fr 280px", gap: 12, minHeight: 420 }}>
            <div style={{ border: `1px solid ${token.colorBorderSecondary}`, borderRadius: 8, padding: 12, overflow: "auto" }}>
              {grantView === "tree" ? (
                <Tree
                  checkable
                  selectable={false}
                  checkedKeys={grantCheckedKeys}
                  expandedKeys={grantExpandedKeys}
                  onExpand={(keys) => setGrantExpandedKeys((keys || []).map((k) => String(k)))}
                  onCheck={(keys) => {
                    const list = Array.isArray(keys) ? keys : keys?.checked || [];
                    setGrantCheckedKeys((list || []).map((k) => String(k)));
                  }}
                  treeData={filteredGrantTree}
                  loading={menuLoading}
                />
              ) : (
                <div>
                  {(grantCheckedKeys || []).length === 0 ? (
                    <Typography.Text type="secondary">未选择任何菜单</Typography.Text>
                  ) : (
                    <Space wrap>
                      {grantCheckedKeys.map((k) => (
                        <Tag key={k} closable onClose={(e) => {
                          e.preventDefault();
                          setGrantCheckedKeys(grantCheckedKeys.filter((x) => x !== k));
                        }}>
                          {menuLabelById.get(k) || k}
                        </Tag>
                      ))}
                    </Space>
                  )}
                </div>
              )}
            </div>
            <div style={{ border: `1px solid ${token.colorBorderSecondary}`, borderRadius: 8, padding: 12, overflow: "auto" }}>
              <Typography.Text strong>本次变更</Typography.Text>
              <Divider style={{ margin: "10px 0" }} />
              <Typography.Text type="success">新增 {addedMenuKeys.length}</Typography.Text>
              <div style={{ marginTop: 6, marginBottom: 10 }}>
                {(addedMenuKeys || []).length === 0 ? (
                  <Typography.Text type="secondary">无</Typography.Text>
                ) : (
                  <Space wrap>
                    {addedMenuKeys.map((k) => <Tag color="success" key={`a-${k}`}>{menuLabelById.get(k) || k}</Tag>)}
                  </Space>
                )}
              </div>
              <Typography.Text type="danger">移除 {removedMenuKeys.length}</Typography.Text>
              <div style={{ marginTop: 6 }}>
                {(removedMenuKeys || []).length === 0 ? (
                  <Typography.Text type="secondary">无</Typography.Text>
                ) : (
                  <Space wrap>
                    {removedMenuKeys.map((k) => <Tag color="error" key={`r-${k}`}>{menuLabelById.get(k) || k}</Tag>)}
                  </Space>
                )}
              </div>
            </div>
          </div>
        </Space>
      </Modal>
      <Drawer
        title="角色详情"
        width={560}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        destroyOnHidden
      >
        {currentRole ? (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="角色ID">{currentRole.roleId}</Descriptions.Item>
            <Descriptions.Item label="角色名称">{currentRole.name || "—"}</Descriptions.Item>
            <Descriptions.Item label="角色编码">{currentRole.code || "—"}</Descriptions.Item>
            <Descriptions.Item label="状态">{renderStatusTag(currentRole.status)}</Descriptions.Item>
            <Descriptions.Item label="类型">{currentRole.builtin ? "内置" : "自定义"}</Descriptions.Item>
            <Descriptions.Item label="描述">{currentRole.description || "—"}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatTs(currentRole.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{formatTs(currentRole.updatedAt)}</Descriptions.Item>
            <Descriptions.Item label="删除时间">{formatTs(currentRole.deletedAt)}</Descriptions.Item>
          </Descriptions>
        ) : null}
      </Drawer>
    </PageShell>
  );
}

export default RolePage;
