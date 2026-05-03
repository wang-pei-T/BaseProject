import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Descriptions,
  Drawer,
  Dropdown,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Tag,
  message,
} from "antd";
import {
  createMenu,
  deleteMenu,
  disableMenu,
  enableMenu,
  queryMenuTree,
  reorderMenus,
  restoreMenu,
  updateMenu,
} from "../../api/menu";
import MenuIconPicker from "../../components/MenuIconPicker";
import PageShell from "../../components/page/PageShell";
import QueryBar from "../../components/page/QueryBar";
import TableCard from "../../components/page/TableCard";
import { exportSheet } from "../../utils/exportExcel";
import { renderStatusTag, statusLabelText } from "../../utils/tableDisplay";
import { getMenuIcon } from "../../config/menu-icons";
import useAppStore from "../../store/app-store";

const emptyDraft = () => ({ name: "", type: "", status: "", includeDeleted: "0", includeDisabled: "1" });

const MENU_TYPE_LABEL = { DIR: "目录", PAGE: "页面", ACTION: "操作点", LINK: "外链" };

function errMsg(e) {
  return e?.response?.data?.message || e?.message || "操作失败";
}

function menuTypeLabel(t) {
  return MENU_TYPE_LABEL[t] || t || "—";
}

function collectMenus(nodes, out = []) {
  (nodes || []).forEach((n) => {
    out.push(n);
    if (n.children && n.children.length) collectMenus(n.children, out);
  });
  return out;
}

function findMenuNode(menuId, nodes) {
  for (const n of nodes || []) {
    if (String(n.menuId) === String(menuId)) return n;
    const c = findMenuNode(menuId, n.children);
    if (c) return c;
  }
  return null;
}

function collectDescendantIds(menuId, nodes) {
  const root = findMenuNode(menuId, nodes);
  if (!root || !root.children) return new Set();
  const found = [];
  const stack = [...root.children];
  while (stack.length) {
    const c = stack.pop();
    found.push(String(c.menuId));
    (c.children || []).forEach((x) => stack.push(x));
  }
  return new Set(found);
}

function buildMenuRowTree(flat) {
  const byId = new Map();
  (flat || []).forEach((m) => {
    byId.set(String(m.menuId), { ...m, children: [] });
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
      if (n.children && n.children.length) prune(n.children);
      else delete n.children;
    });
  };
  prune(roots);
  return roots;
}

function flattenMenuRows(nodes, acc = []) {
  (nodes || []).forEach((n) => {
    const { children, ...rest } = n;
    acc.push(rest);
    if (children && children.length) flattenMenuRows(children, acc);
  });
  return acc;
}

function MenuPage() {
  const bumpSidebarMenu = useAppStore((s) => s.bumpSidebarMenu);
  const [rawRoots, setRawRoots] = useState([]);
  const [loading, setLoading] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [reorderOpen, setReorderOpen] = useState(false);
  const [reorderBase, setReorderBase] = useState(null);
  const [reorderDraft, setReorderDraft] = useState([]);
  const [currentRow, setCurrentRow] = useState(null);
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const [draftQuery, setDraftQuery] = useState(emptyDraft);
  const [activeQuery, setActiveQuery] = useState(emptyDraft);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await queryMenuTree({
        includeDeleted: activeQuery.includeDeleted === "1",
        includeDisabled: activeQuery.includeDisabled === "1",
      });
      setRawRoots(response.data?.data?.items || []);
    } catch (e) {
      message.error(errMsg(e));
    } finally {
      setLoading(false);
    }
  }, [activeQuery.includeDeleted, activeQuery.includeDisabled]);

  useEffect(() => {
    load();
  }, [load]);

  const flatAll = useMemo(() => collectMenus(rawRoots, []), [rawRoots]);

  const filteredFlat = useMemo(() => {
    return flatAll.filter((item) => {
      if (activeQuery.name && !(item.name || "").includes(activeQuery.name)) return false;
      if (activeQuery.status && item.status !== activeQuery.status) return false;
      if (activeQuery.type && item.menuType !== activeQuery.type) return false;
      return true;
    });
  }, [flatAll, activeQuery.name, activeQuery.status, activeQuery.type]);

  const treeData = useMemo(() => buildMenuRowTree(filteredFlat), [filteredFlat]);

  const parentOptions = useMemo(() => {
    const editingId = editOpen ? currentRow?.menuId : null;
    const block = editingId ? collectDescendantIds(editingId, rawRoots) : new Set();
    return flatAll
      .filter((m) => !m.deletedAt)
      .filter((m) => !editingId || (String(m.menuId) !== String(editingId) && !block.has(String(m.menuId))))
      .map((m) => ({ value: m.menuId, label: `${m.name || m.menuId} (${m.menuId})` }));
  }, [flatAll, rawRoots, editOpen, currentRow]);

  const onSearch = () => {
    setActiveQuery({ ...draftQuery });
  };

  const resetQuery = () => {
    const q = emptyDraft();
    setDraftQuery(q);
    setActiveQuery(q);
  };

  const onCreateSubmit = async () => {
    const values = await createForm.validateFields();
    const icon = values.icon != null && String(values.icon).trim() !== "" ? String(values.icon).trim() : undefined;
    await createMenu({
      ...values,
      parentId: values.parentId || undefined,
      ...(icon !== undefined ? { icon } : {}),
    });
    createForm.resetFields();
    setCreateOpen(false);
    message.success("已创建");
    bumpSidebarMenu();
    load();
  };

  const onEditSubmit = async () => {
    const values = await editForm.validateFields();
    await updateMenu(currentRow.menuId, {
      ...values,
      parentId: values.parentId || null,
      icon: values.icon != null && String(values.icon).trim() !== "" ? String(values.icon).trim() : null,
    });
    setEditOpen(false);
    setCurrentRow(null);
    message.success("已保存");
    bumpSidebarMenu();
    load();
  };

  const handleExport = () => {
    const flat = flattenMenuRows(treeData, []);
    const headers = ["菜单名称", "类型", "路由路径", "排序", "状态", "侧边隐藏", "删除"];
    const rows = flat.map((r) => [
      r.name || "",
      menuTypeLabel(r.menuType),
      r.path || "",
      r.sort ?? "",
      statusLabelText(r.status),
      r.hidden ? "是" : "否",
      r.deletedAt != null ? "已删" : "",
    ]);
    exportSheet("菜单树", "菜单", headers, rows);
    message.success("已导出");
  };

  const openReorder = (row) => {
    const sid = String(row.parentId ?? "");
    const sibs = flatAll.filter((r) => !r.deletedAt && String(r.parentId ?? "") === sid);
    const sorted = [...sibs].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));
    setReorderBase(row);
    setReorderDraft(sorted.map((r) => ({ menuId: r.menuId, sort: r.sort ?? 0 })));
    setReorderOpen(true);
  };

  const submitReorder = async () => {
    await reorderMenus({ items: reorderDraft.map((x) => ({ menuId: x.menuId, sort: x.sort })) });
    setReorderOpen(false);
    setReorderBase(null);
    message.success("排序已保存");
    bumpSidebarMenu();
    load();
  };

  const openEdit = (row) => {
    setCurrentRow(row);
    editForm.setFieldsValue({
      name: row.name,
      path: row.path,
      icon: row.icon,
      sort: row.sort,
      status: row.status,
      menuType: row.menuType || "PAGE",
      parentId: row.parentId != null ? String(row.parentId) : undefined,
    });
    setEditOpen(true);
  };

  const toggleHidden = (row) => {
    updateMenu(row.menuId, { hidden: !row.hidden })
      .then(() => {
        message.success(row.hidden ? "已取消侧边隐藏" : "已设为侧边隐藏");
        bumpSidebarMenu();
        load();
      })
      .catch((e) => message.error(errMsg(e)));
  };

  const columns = [
    { title: "菜单名称", dataIndex: "name", ellipsis: true, render: (t) => t || "-" },
    {
      title: "图标",
      dataIndex: "icon",
      width: 56,
      align: "center",
      render: (ic) => getMenuIcon(ic) || "—",
    },
    { title: "类型", dataIndex: "menuType", width: 100, render: (x) => menuTypeLabel(x) },
    { title: "路由路径", dataIndex: "path", ellipsis: true, render: (t) => t || "-" },
    { title: "排序", dataIndex: "sort", width: 72, render: (t) => t ?? "-" },
    { title: "状态", dataIndex: "status", width: 100, render: (s) => renderStatusTag(s) },
    {
      title: "侧边隐藏",
      dataIndex: "hidden",
      width: 100,
      render: (v) => (v ? <Tag color="warning">是</Tag> : <Tag>否</Tag>),
    },
    {
      title: "删除",
      width: 72,
      render: (_, r) => (r.deletedAt != null ? <Tag color="default">已删</Tag> : "—"),
    },
    {
      title: "操作",
      width: 300,
      fixed: "right",
      render: (_, row) => {
        const deleted = row.deletedAt != null;
        if (deleted) {
          return (
            <Space size={0} wrap>
              <Button
                type="link"
                size="small"
                onClick={() => {
                  setCurrentRow(row);
                  setDetailOpen(true);
                }}
              >
                详情
              </Button>
              <Dropdown
                menu={{
                  items: [{ key: "restore", label: "恢复" }],
                  onClick: ({ key }) => {
                    if (key === "restore") {
                      Modal.confirm({
                        title: "恢复该菜单？",
                        onOk: () =>
                          restoreMenu(row.menuId).then(() => {
                            message.success("已恢复");
                            bumpSidebarMenu();
                            load();
                          }).catch((e) => message.error(errMsg(e))),
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
        return (
          <Space size={0} wrap>
            <Button
              type="link"
              size="small"
              onClick={() => {
                setCurrentRow(row);
                setDetailOpen(true);
              }}
            >
              详情
            </Button>
            <Button type="link" size="small" onClick={() => openEdit(row)}>
              编辑
            </Button>
            {row.status === "DISABLED" ? (
              <Button
                type="link"
                size="small"
                onClick={() =>
                  enableMenu(row.menuId).then(() => {
                    message.success("已启用");
                    bumpSidebarMenu();
                    load();
                  }).catch((e) => message.error(errMsg(e)))
                }
              >
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
                    onOk: () =>
                      disableMenu(row.menuId).then(() => {
                        message.success("已禁用");
                        bumpSidebarMenu();
                        load();
                      }).catch((e) => message.error(errMsg(e))),
                  });
                }}
              >
                禁用
              </Button>
            )}
            <Dropdown
              menu={{
                items: [
                  { key: "reorder", label: "同级排序" },
                  { key: "hidden", label: row.hidden ? "取消侧边隐藏" : "侧边隐藏" },
                  { type: "divider" },
                  { key: "delete", label: "删除", danger: true },
                ],
                onClick: ({ key }) => {
                  if (key === "reorder") openReorder(row);
                  if (key === "hidden") toggleHidden(row);
                  if (key === "delete") {
                    Modal.confirm({
                      title: "确认删除（软删）？",
                      onOk: () =>
                        deleteMenu(row.menuId).then(() => {
                          message.success("已删除");
                          bumpSidebarMenu();
                          load();
                        }).catch((e) => message.error(errMsg(e))),
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
    <PageShell title="菜单管理">
      <QueryBar onSearch={onSearch} onReset={resetQuery} loading={loading}>
        <Form.Item label="菜单名称">
          <Input
            allowClear
            placeholder="请输入菜单名称"
            value={draftQuery.name}
            onChange={(e) => setDraftQuery((p) => ({ ...p, name: e.target.value }))}
            style={{ width: 200 }}
          />
        </Form.Item>
        <Form.Item label="菜单类型">
          <Select
            allowClear
            placeholder="全部"
            style={{ width: 130 }}
            value={draftQuery.type || undefined}
            onChange={(v) => setDraftQuery((p) => ({ ...p, type: v || "" }))}
            options={[
              { value: "DIR", label: "目录" },
              { value: "PAGE", label: "页面" },
              { value: "ACTION", label: "操作点" },
              { value: "LINK", label: "外链" },
            ]}
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
        <Form.Item label="含已禁用">
          <Select
            style={{ width: 100 }}
            value={draftQuery.includeDisabled}
            onChange={(v) => setDraftQuery((p) => ({ ...p, includeDisabled: v }))}
            options={[
              { value: "1", label: "是" },
              { value: "0", label: "否" },
            ]}
          />
        </Form.Item>
      </QueryBar>
      <TableCard
        title="菜单树"
        rowKey={(r) => r.menuId}
        loading={loading}
        columns={columns}
        dataSource={treeData}
        pagination={false}
        scroll={{ x: 1140 }}
        defaultExpandAllRows
        extra={
          <Space wrap>
            <Button
              type="primary"
              onClick={() => {
                createForm.resetFields();
                createForm.setFieldsValue({ menuType: "PAGE", sort: 0, status: "ENABLED" });
                setCreateOpen(true);
              }}
            >
              新增菜单
            </Button>
            <Button onClick={handleExport}>导出 Excel</Button>
          </Space>
        }
      />
      <Modal
        title="新增菜单"
        open={createOpen}
        onCancel={() => {
          setCreateOpen(false);
          createForm.resetFields();
        }}
        onOk={onCreateSubmit}
        destroyOnClose
        width={560}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="name" label="菜单名称" rules={[{ required: true, message: "请输入菜单名称" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="parentId" label="上级菜单">
            <Select allowClear showSearch optionFilterProp="label" options={parentOptions} placeholder="根" />
          </Form.Item>
          <Form.Item name="path" label="路由路径">
            <Input placeholder="/example" />
          </Form.Item>
          <Form.Item name="icon" label="图标">
            <MenuIconPicker />
          </Form.Item>
          <Form.Item name="menuType" label="类型" initialValue="PAGE">
            <Select
              options={[
                { value: "DIR", label: "目录" },
                { value: "PAGE", label: "页面" },
                { value: "ACTION", label: "操作点" },
                { value: "LINK", label: "外链" },
              ]}
            />
          </Form.Item>
          <Form.Item name="sort" label="排序" initialValue={0}>
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue="ENABLED">
            <Select
              options={[
                { value: "ENABLED", label: "启用" },
                { value: "DISABLED", label: "禁用" },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="编辑菜单"
        open={editOpen}
        onCancel={() => {
          setEditOpen(false);
          setCurrentRow(null);
        }}
        onOk={onEditSubmit}
        destroyOnClose
        width={560}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item name="name" label="菜单名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="parentId" label="上级菜单">
            <Select allowClear showSearch optionFilterProp="label" options={parentOptions} placeholder="根" />
          </Form.Item>
          <Form.Item name="path" label="路由路径">
            <Input />
          </Form.Item>
          <Form.Item name="icon" label="图标">
            <MenuIconPicker />
          </Form.Item>
          <Form.Item name="menuType" label="类型">
            <Select
              options={[
                { value: "DIR", label: "目录" },
                { value: "PAGE", label: "页面" },
                { value: "ACTION", label: "操作点" },
                { value: "LINK", label: "外链" },
              ]}
            />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { value: "ENABLED", label: "启用" },
                { value: "DISABLED", label: "禁用" },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title="同级排序"
        open={reorderOpen}
        onCancel={() => {
          setReorderOpen(false);
          setReorderBase(null);
        }}
        onOk={submitReorder}
        width={480}
      >
        <p style={{ marginBottom: 12 }}>调整与「{reorderBase?.name}」同级的菜单 sort 值（数值越小越靠前）。</p>
        <Space direction="vertical" style={{ width: "100%" }}>
          {reorderDraft.map((line, idx) => (
            <Space key={String(line.menuId)} style={{ width: "100%", justifyContent: "space-between" }}>
              <span>{flatAll.find((m) => String(m.menuId) === String(line.menuId))?.name || line.menuId}</span>
              <InputNumber
                value={line.sort}
                onChange={(v) => {
                  const next = [...reorderDraft];
                  next[idx] = { ...next[idx], sort: v ?? 0 };
                  setReorderDraft(next);
                }}
              />
            </Space>
          ))}
        </Space>
      </Modal>
      <Drawer
        title="菜单详情"
        open={detailOpen}
        onClose={() => {
          setDetailOpen(false);
          setCurrentRow(null);
        }}
        width={420}
      >
        {currentRow && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="ID">{currentRow.menuId}</Descriptions.Item>
            <Descriptions.Item label="名称">{currentRow.name}</Descriptions.Item>
            <Descriptions.Item label="上级">{currentRow.parentId ?? "—"}</Descriptions.Item>
            <Descriptions.Item label="路径">{currentRow.path || "—"}</Descriptions.Item>
            <Descriptions.Item label="图标">{getMenuIcon(currentRow.icon) || "—"}</Descriptions.Item>
            <Descriptions.Item label="类型">{menuTypeLabel(currentRow.menuType)}</Descriptions.Item>
            <Descriptions.Item label="排序">{currentRow.sort ?? "—"}</Descriptions.Item>
            <Descriptions.Item label="状态">{statusLabelText(currentRow.status)}</Descriptions.Item>
            <Descriptions.Item label="侧边隐藏">{currentRow.hidden ? "是" : "否"}</Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </PageShell>
  );
}

export default MenuPage;
