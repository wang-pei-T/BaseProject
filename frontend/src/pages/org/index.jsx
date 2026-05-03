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
  Segmented,
  Select,
  TreeSelect,
  Space,
  Table,
  Tag,
  message,
  theme,
} from "antd";
import {
  createOrg,
  deleteOrg,
  disableOrg,
  enableOrg,
  getOrg,
  moveOrg,
  moveOrgUsers,
  queryOrgTree,
  restoreOrg,
  updateOrg,
} from "../../api/org";
import { queryUsers } from "../../api/user";
import PageShell from "../../components/page/PageShell";
import QueryBar from "../../components/page/QueryBar";
import TableCard from "../../components/page/TableCard";
import { exportSheet } from "../../utils/exportExcel";
import { formatTs, renderStatusTag, statusLabelText } from "../../utils/tableDisplay";

const emptyDraft = () => ({
  name: "",
  code: "",
  status: "",
  parentOrgId: "",
  includeDeleted: "0",
});

function errMsg(e) {
  return e?.response?.data?.message || e?.message || "操作失败";
}

function friendlyCode(msg) {
  const m = {
    ORG_HAS_CHILDREN: "存在下级机构，无法删除",
    ORG_HAS_USERS: "存在归属用户，无法删除",
    ORG_MOVE_CYCLE: "不能移动到自身或其下级机构下",
    RESTORE_CONFLICT_UNIQUE: "恢复失败：机构编码与现存机构冲突",
    ORG_DISABLED: "目标机构已禁用，无法迁入",
    CONFLICT_UNIQUE: "编码已存在",
    VALIDATION_ERROR: "参数校验失败",
  };
  return m[msg] || msg;
}

function buildOrgTree(flat) {
  const byId = new Map();
  flat.forEach((o) => {
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
  const depthRec = (nodes, d = 1) => {
    nodes.forEach((n) => {
      n.depth = d;
      if (n.children?.length) depthRec(n.children, d + 1);
    });
  };
  depthRec(roots);
  return roots;
}

function collectDescendantOrgIds(orgId, flat) {
  const id = String(orgId);
  const childrenMap = new Map();
  flat.forEach((o) => {
    const pid = o.parentOrgId != null ? String(o.parentOrgId) : "";
    if (!childrenMap.has(pid)) childrenMap.set(pid, []);
    childrenMap.get(pid).push(String(o.orgId));
  });
  const out = new Set();
  const stack = [...(childrenMap.get(id) || [])];
  while (stack.length) {
    const cur = stack.pop();
    out.add(cur);
    (childrenMap.get(cur) || []).forEach((c) => stack.push(c));
  }
  return out;
}

function flattenOrgRows(nodes, depth = 1, acc = []) {
  (nodes || []).forEach((n) => {
    acc.push({ ...n, depth });
    if (n.children?.length) flattenOrgRows(n.children, depth + 1, acc);
  });
  return acc;
}

function mapOrgNodesToTreeSelect(nodes, isDisabled) {
  return (nodes || []).map((n) => {
    const id = String(n.orgId);
    return {
      value: id,
      title: `${n.name || id}${n.code ? ` (${n.code})` : ""}`,
      disabled: isDisabled(id),
      children: n.children?.length ? mapOrgNodesToTreeSelect(n.children, isDisabled) : undefined,
    };
  });
}

function OrgPage() {
  const { token } = theme.useToken();
  const [raw, setRaw] = useState([]);
  const [loading, setLoading] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editOrgId, setEditOrgId] = useState(null);
  const [moveOpen, setMoveOpen] = useState(false);
  const [moveOrgId, setMoveOrgId] = useState(null);
  const [moveInitialParentId, setMoveInitialParentId] = useState("__root__");
  const [migrateOpen, setMigrateOpen] = useState(false);
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const [moveForm] = Form.useForm();
  const [draftQuery, setDraftQuery] = useState(emptyDraft);
  const [activeQuery, setActiveQuery] = useState(emptyDraft);
  const [sourceOrgId, setSourceOrgId] = useState("");
  const [targetOrgId, setTargetOrgId] = useState("");
  const [memberUsers, setMemberUsers] = useState([]);
  const [memberLoading, setMemberLoading] = useState(false);
  const [selectedUserIds, setSelectedUserIds] = useState([]);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailOrgId, setDetailOrgId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailUsers, setDetailUsers] = useState([]);
  const [detailUserTotal, setDetailUserTotal] = useState(0);
  const [detailUserPage, setDetailUserPage] = useState(1);
  const [detailUsersLoading, setDetailUsersLoading] = useState(false);
  const [orgUsersOpen, setOrgUsersOpen] = useState(false);
  const [orgUsersOrgId, setOrgUsersOrgId] = useState(null);
  const [orgUsersOrgName, setOrgUsersOrgName] = useState("");
  const [orgUsersKeyword, setOrgUsersKeyword] = useState("");
  const [orgUsersSubtree, setOrgUsersSubtree] = useState(false);
  const [orgUsersPage, setOrgUsersPage] = useState(1);
  const orgUsersPageSize = 10;
  const [orgUsersTotal, setOrgUsersTotal] = useState(0);
  const [orgUsersRows, setOrgUsersRows] = useState([]);
  const [orgUsersLoading, setOrgUsersLoading] = useState(false);
  const [orgUsersSearchTick, setOrgUsersSearchTick] = useState(0);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await queryOrgTree({
        includeDisabled: true,
        includeDeleted: activeQuery.includeDeleted === "1",
      });
      setRaw(response.data?.data?.items || []);
    } catch (e) {
      message.error(errMsg(e));
    } finally {
      setLoading(false);
    }
  }, [activeQuery.includeDeleted]);

  useEffect(() => {
    load();
  }, [load]);

  const parentOrgOptions = useMemo(() => {
    const m = new Map();
    raw.forEach((r) => {
      const id = r.orgId != null ? String(r.orgId) : "";
      if (id) m.set(id, { value: id, label: r.name || id });
    });
    return [...m.values()];
  }, [raw]);

  const orgSelectOptions = useMemo(
    () =>
      raw.map((o) => ({
        value: String(o.orgId),
        label: `${o.name || ""} (${o.code || o.orgId})`,
      })),
    [raw],
  );

  const nameById = useMemo(() => {
    const m = new Map();
    raw.forEach((r) => {
      if (r.orgId != null) m.set(String(r.orgId), r.name || r.orgId);
    });
    return m;
  }, [raw]);

  const items = useMemo(() => {
    return raw.filter((item) => {
      if (activeQuery.name && !(item.name || "").includes(activeQuery.name)) return false;
      if (activeQuery.code && !(item.code || "").includes(activeQuery.code)) return false;
      if (activeQuery.status && item.status !== activeQuery.status) return false;
      if (activeQuery.parentOrgId && String(item.parentOrgId || "") !== String(activeQuery.parentOrgId)) return false;
      return true;
    });
  }, [raw, activeQuery]);

  const treeData = useMemo(() => buildOrgTree(items), [items]);

  const parentOrgTreeData = useMemo(() => {
    const roots = buildOrgTree(raw);
    return mapOrgNodesToTreeSelect(roots, () => false);
  }, [raw]);

  const moveParentTreeData = useMemo(() => {
    if (!moveOrgId) return [];
    const blocked = new Set([String(moveOrgId), ...collectDescendantOrgIds(moveOrgId, raw)]);
    const roots = buildOrgTree(raw);
    const mappedRoots = mapOrgNodesToTreeSelect(roots, (id) => blocked.has(id));
    return [{ value: "__root__", title: "（根节点）", children: mappedRoots }];
  }, [moveOrgId, raw]);

  const loadMemberUsers = async () => {
    if (!sourceOrgId) {
      message.warning("请选择源机构");
      return;
    }
    setMemberLoading(true);
    try {
      const pageSize = 500;
      let page = 1;
      let all = [];
      let total = 0;
      for (;;) {
        const res = await queryUsers({ page, pageSize, orgId: sourceOrgId });
        const d = res.data?.data || {};
        const chunk = d.items || [];
        total = Number(d.total) || 0;
        all = all.concat(chunk);
        if (chunk.length < pageSize || all.length >= total || all.length >= 500) break;
        page += 1;
      }
      setMemberUsers(all.slice(0, 500));
      setSelectedUserIds([]);
      if (total > 500) message.warning("仅展示前 500 人，请缩小范围或分批迁移");
    } catch (e) {
      message.error(errMsg(e));
    } finally {
      setMemberLoading(false);
    }
  };

  const closeMigrateModal = () => {
    setMigrateOpen(false);
    setSourceOrgId("");
    setTargetOrgId("");
    setMemberUsers([]);
    setSelectedUserIds([]);
  };

  const onMoveMembers = async () => {
    if (!targetOrgId || selectedUserIds.length === 0) {
      message.warning("请选择目标机构并勾选用户");
      return;
    }
    try {
      await moveOrgUsers(targetOrgId, selectedUserIds.map((id) => String(id)));
      message.success("已提交迁移");
      loadMemberUsers();
      load();
    } catch (e) {
      message.error(friendlyCode(errMsg(e)) || errMsg(e));
    }
  };

  const memberColumns = [
    { title: "姓名", render: (_, r) => r.displayName || r.username },
    { title: "账号", dataIndex: "username" },
  ];

  const onSearch = () => {
    setActiveQuery({ ...draftQuery });
  };

  const resetQuery = () => {
    const q = emptyDraft();
    setDraftQuery(q);
    setActiveQuery(q);
  };

  const onCreateSubmit = async () => {
    try {
      const values = await createForm.validateFields();
      const payload = {
        name: values.name,
        code: values.code || undefined,
        parentOrgId: values.parentOrgId || undefined,
        status: values.status || "ENABLED",
        sort: values.sort ?? 1,
      };
      await createOrg(payload);
      createForm.resetFields();
      setCreateOpen(false);
      message.success("已创建");
      load();
    } catch (e) {
      if (e?.errorFields) throw e;
      message.error(friendlyCode(errMsg(e)) || errMsg(e));
      throw e;
    }
  };

  const openEdit = async (orgId) => {
    setEditOrgId(orgId);
    try {
      const res = await getOrg(orgId, { includeDeleted: false });
      const o = res.data?.data;
      editForm.setFieldsValue({
        name: o?.name,
        code: o?.code,
        sort: o?.sort ?? 0,
      });
      setEditOpen(true);
    } catch (e) {
      message.error(errMsg(e));
    }
  };

  const onEditSubmit = async () => {
    try {
      const v = await editForm.validateFields();
      await updateOrg(editOrgId, { name: v.name, code: v.code || undefined, sort: v.sort });
      message.success("已保存");
      setEditOpen(false);
      setEditOrgId(null);
      load();
    } catch (e) {
      if (e?.errorFields) throw e;
      message.error(friendlyCode(errMsg(e)) || errMsg(e));
      throw e;
    }
  };

  const openMove = (orgId, parentOrgId) => {
    const pid = parentOrgId != null && String(parentOrgId) !== "" ? String(parentOrgId) : "__root__";
    setMoveOrgId(orgId);
    setMoveInitialParentId(pid);
    setMoveOpen(true);
  };

  useEffect(() => {
    if (!moveOpen || moveOrgId == null) return;
    moveForm.setFieldsValue({ newParentOrgId: moveInitialParentId });
  }, [moveOpen, moveOrgId, moveInitialParentId, moveForm]);

  const onMoveSubmit = async () => {
    try {
      const v = await moveForm.validateFields();
      const np = v.newParentOrgId;
      const body = { newParentOrgId: np === "__root__" || np === undefined || np === "" ? null : np };
      await moveOrg(moveOrgId, body);
      message.success("已调整上级");
      setMoveOpen(false);
      setMoveOrgId(null);
      setMoveInitialParentId("__root__");
      load();
    } catch (e) {
      if (e?.errorFields) throw e;
      message.error(friendlyCode(errMsg(e)) || errMsg(e));
      throw e;
    }
  };

  const loadDetailUsers = useCallback(async () => {
    if (!detailOrgId || !detailOpen) return;
    setDetailUsersLoading(true);
    try {
      const res = await queryUsers({
        page: detailUserPage,
        pageSize: 10,
        orgId: detailOrgId,
        includeDeleted: false,
      });
      const d = res.data?.data || {};
      setDetailUsers(d.items || []);
      setDetailUserTotal(Number(d.total) || 0);
    } catch (e) {
      message.error(errMsg(e));
    } finally {
      setDetailUsersLoading(false);
    }
  }, [detailOrgId, detailOpen, detailUserPage]);

  useEffect(() => {
    if (!detailOpen || !detailOrgId) return;
    let cancelled = false;
    (async () => {
      setDetailLoading(true);
      try {
        const res = await getOrg(detailOrgId, { includeDeleted: activeQuery.includeDeleted === "1" });
        if (!cancelled) setDetail(res.data?.data || null);
      } catch (e) {
        if (!cancelled) {
          message.error(errMsg(e));
          setDetail(null);
        }
      } finally {
        if (!cancelled) setDetailLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [detailOpen, detailOrgId, activeQuery.includeDeleted]);

  useEffect(() => {
    setDetailUserPage(1);
  }, [detailOrgId]);

  useEffect(() => {
    loadDetailUsers();
  }, [loadDetailUsers]);

  const loadOrgUsers = useCallback(async () => {
    if (!orgUsersOpen || orgUsersOrgId == null) return;
    setOrgUsersLoading(true);
    try {
      const res = await queryUsers({
        page: orgUsersPage,
        pageSize: orgUsersPageSize,
        orgId: String(orgUsersOrgId),
        keyword: orgUsersKeyword.trim() || undefined,
        includeDeleted: false,
        includeOrgSubtree: orgUsersSubtree,
      });
      const d = res.data?.data || {};
      setOrgUsersRows(d.items || []);
      setOrgUsersTotal(Number(d.total) || 0);
    } catch (e) {
      message.error(errMsg(e));
      setOrgUsersRows([]);
      setOrgUsersTotal(0);
    } finally {
      setOrgUsersLoading(false);
    }
  }, [orgUsersOpen, orgUsersOrgId, orgUsersPage, orgUsersKeyword, orgUsersSubtree, orgUsersSearchTick]);

  useEffect(() => {
    loadOrgUsers();
  }, [loadOrgUsers]);

  const openOrgUsers = (item) => {
    setOrgUsersOrgId(item.orgId);
    setOrgUsersOrgName(item.name || String(item.orgId));
    setOrgUsersKeyword("");
    setOrgUsersSubtree(false);
    setOrgUsersPage(1);
    setOrgUsersSearchTick(0);
    setOrgUsersOpen(true);
  };

  const closeOrgUsersModal = () => {
    setOrgUsersOpen(false);
    setOrgUsersOrgId(null);
    setOrgUsersOrgName("");
    setOrgUsersKeyword("");
    setOrgUsersSubtree(false);
    setOrgUsersPage(1);
    setOrgUsersSearchTick(0);
    setOrgUsersRows([]);
    setOrgUsersTotal(0);
  };

  const detailSubOrgs = useMemo(() => {
    if (!detail?.orgId) return [];
    const id = String(detail.orgId);
    return raw.filter((r) => String(r.parentOrgId || "") === id);
  }, [detail, raw]);

  const handleExport = () => {
    const flat = flattenOrgRows(treeData);
    const headers = ["序号", "机构名称", "机构编码", "层级", "上级机构", "人员数", "状态", "排序", "创建时间", "删除"];
    const rows = flat.map((r, i) => [
      i + 1,
      r.name || "",
      r.code || "",
      r.depth ? `${r.depth}级` : "",
      nameById.get(String(r.parentOrgId || "")) || r.parentOrgId || "",
      r.userCount ?? "",
      statusLabelText(r.status),
      r.sort ?? "",
      formatTs(r.createdAt),
      r.deletedAt != null ? "已删" : "",
    ]);
    exportSheet("机构列表", "机构", headers, rows);
    message.success("已导出");
  };

  const columns = [
    { title: "机构名称", dataIndex: "name", ellipsis: true, render: (t) => t || "-" },
    { title: "机构编码", dataIndex: "code", width: 120, render: (t) => t || "-" },
    { title: "层级", width: 72, render: (_, r) => `${r.depth ?? 1}级` },
    { title: "上级机构", width: 140, ellipsis: true, render: (_, r) => nameById.get(String(r.parentOrgId || "")) || r.parentOrgId || "—" },
    { title: "人员数", dataIndex: "userCount", width: 80, render: (t) => t ?? "-" },
    { title: "状态", dataIndex: "status", width: 100, render: (s) => renderStatusTag(s) },
    { title: "排序", dataIndex: "sort", width: 72, render: (t) => t ?? "-" },
    {
      title: "操作",
      width: 300,
      fixed: "right",
      render: (_, item) => {
        const deleted = item.deletedAt != null;
        if (deleted) {
          return (
            <Space size={0} wrap>
              <Button
                type="link"
                size="small"
                onClick={() => {
                  setDetailOrgId(item.orgId);
                  setDetail(null);
                  setDetailUserPage(1);
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
                        title: "确认恢复该机构？",
                        onOk: () =>
                          restoreOrg(item.orgId)
                            .then(() => {
                              message.success("已恢复");
                              load();
                            })
                            .catch((e) => message.error(friendlyCode(errMsg(e)) || errMsg(e))),
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
                setDetailOrgId(item.orgId);
                setDetail(null);
                setDetailUserPage(1);
                setDetailOpen(true);
              }}
            >
              详情
            </Button>
            <Button type="link" size="small" onClick={() => openEdit(item.orgId)}>
              编辑
            </Button>
            {item.status === "DISABLED" ? (
              <Button type="link" size="small" onClick={() => enableOrg(item.orgId).then(() => { message.success("已启用"); load(); }).catch((e) => message.error(errMsg(e)))}>
                启用
              </Button>
            ) : (
              <Button
                type="link"
                size="small"
                danger
                onClick={() => {
                  Modal.confirm({
                    title: "确认禁用该机构？",
                    onOk: () => disableOrg(item.orgId).then(() => { message.success("已禁用"); load(); }).catch((e) => message.error(errMsg(e))),
                  });
                }}
              >
                禁用
              </Button>
            )}
            <Dropdown
              menu={{
                items: [
                  { key: "users", label: "机构用户" },
                  { key: "move", label: "调整上级" },
                  { type: "divider" },
                  { key: "delete", label: "删除", danger: true },
                ],
                onClick: ({ key }) => {
                  if (key === "users") openOrgUsers(item);
                  if (key === "move") openMove(item.orgId, item.parentOrgId);
                  if (key === "delete") {
                    Modal.confirm({
                      title: "确认软删除该机构？",
                      content: "需无下级机构且无归属用户。",
                      onOk: () =>
                        deleteOrg(item.orgId)
                          .then(() => {
                            message.success("已删除");
                            load();
                          })
                          .catch((e) => message.error(friendlyCode(errMsg(e)) || errMsg(e))),
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
    <PageShell title="机构管理">
      <QueryBar onSearch={onSearch} onReset={resetQuery} loading={loading}>
        <Form.Item label="机构名称">
          <Input
            allowClear
            placeholder="请输入机构名称"
            value={draftQuery.name}
            onChange={(e) => setDraftQuery((p) => ({ ...p, name: e.target.value }))}
            style={{ width: 200 }}
          />
        </Form.Item>
        <Form.Item label="机构编码">
          <Input
            allowClear
            placeholder="请输入机构编码"
            value={draftQuery.code}
            onChange={(e) => setDraftQuery((p) => ({ ...p, code: e.target.value }))}
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
        <Form.Item label="上级机构">
          <Select
            allowClear
            showSearch
            optionFilterProp="label"
            placeholder="全部"
            style={{ width: 200 }}
            value={draftQuery.parentOrgId || undefined}
            onChange={(v) => setDraftQuery((p) => ({ ...p, parentOrgId: v || "" }))}
            options={parentOrgOptions}
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
        title="机构列表"
        rowKey={(r) => r.orgId}
        loading={loading}
        columns={columns}
        dataSource={treeData}
        pagination={false}
        scroll={{ x: 860 }}
        defaultExpandAllRows
        extra={
          <Space wrap>
            <Button
              type="primary"
              onClick={() => {
                createForm.setFieldsValue({ sort: 1, status: "ENABLED" });
                setCreateOpen(true);
              }}
            >
              新增机构
            </Button>
            <Button
              onClick={() => {
                setSourceOrgId("");
                setTargetOrgId("");
                setMemberUsers([]);
                setSelectedUserIds([]);
                setMigrateOpen(true);
              }}
            >
              批量迁移
            </Button>
            <Button onClick={handleExport}>导出 Excel</Button>
          </Space>
        }
      />
      <Drawer
        title="机构详情"
        width={560}
        open={detailOpen}
        onClose={() => {
          setDetailOpen(false);
          setDetailOrgId(null);
          setDetail(null);
          setDetailUsers([]);
          setDetailUserTotal(0);
        }}
        destroyOnHidden
      >
        {detailLoading ? (
          <div>加载中…</div>
        ) : detail ? (
          <>
            <Descriptions column={1} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="机构ID">{detail.orgId}</Descriptions.Item>
              <Descriptions.Item label="名称">{detail.name}</Descriptions.Item>
              <Descriptions.Item label="编码">{detail.code || "—"}</Descriptions.Item>
              <Descriptions.Item label="上级机构">{nameById.get(String(detail.parentOrgId || "")) || detail.parentOrgId || "—"}</Descriptions.Item>
              <Descriptions.Item label="状态">{renderStatusTag(detail.status)}</Descriptions.Item>
              <Descriptions.Item label="排序">{detail.sort ?? "—"}</Descriptions.Item>
              <Descriptions.Item label="删除时间">{formatTs(detail.deletedAt)}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{formatTs(detail.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{formatTs(detail.updatedAt)}</Descriptions.Item>
            </Descriptions>
            <div style={{ fontWeight: 600, marginBottom: 8 }}>下属机构</div>
            <Table
              size="small"
              rowKey="orgId"
              pagination={false}
              dataSource={detailSubOrgs}
              columns={[
                { title: "名称", dataIndex: "name" },
                { title: "编码", dataIndex: "code", render: (t) => t || "—" },
                { title: "状态", dataIndex: "status", render: (s) => renderStatusTag(s) },
              ]}
              style={{ marginBottom: 16 }}
            />
            <div style={{ fontWeight: 600, marginBottom: 8 }}>本机构人员</div>
            <Table
              size="small"
              rowKey="userId"
              loading={detailUsersLoading}
              dataSource={detailUsers}
              columns={[
                { title: "账号", dataIndex: "username" },
                { title: "姓名", render: (_, r) => r.displayName || "—" },
              ]}
              pagination={{
                current: detailUserPage,
                pageSize: 10,
                total: detailUserTotal,
                showSizeChanger: false,
                onChange: (p) => setDetailUserPage(p),
              }}
            />
          </>
        ) : null}
      </Drawer>
      <Modal
        title={orgUsersOrgName ? `机构用户 — ${orgUsersOrgName}` : "机构用户"}
        open={orgUsersOpen}
        onCancel={closeOrgUsersModal}
        footer={<Button onClick={closeOrgUsersModal}>关闭</Button>}
        width={800}
        destroyOnClose
        styles={{
          body: {
            maxHeight: "min(560px, 72vh)",
            overflow: "auto",
            display: "flex",
            flexDirection: "column",
            gap: 12,
          },
        }}
      >
        <Space wrap style={{ width: "100%", justifyContent: "space-between" }}>
          <Segmented
            value={orgUsersSubtree ? "subtree" : "self"}
            onChange={(v) => {
              setOrgUsersSubtree(v === "subtree");
              setOrgUsersPage(1);
            }}
            options={[
              { value: "self", label: "仅本机构" },
              { value: "subtree", label: "本部及下级机构" },
            ]}
          />
          <Input.Search
            allowClear
            placeholder="账号、姓名、手机或邮箱"
            style={{ width: 280 }}
            value={orgUsersKeyword}
            onChange={(e) => setOrgUsersKeyword(e.target.value)}
            onSearch={() => {
              setOrgUsersPage(1);
              setOrgUsersSearchTick((t) => t + 1);
            }}
            enterButton="搜索"
          />
        </Space>
        <Table
          size="small"
          rowKey="userId"
          loading={orgUsersLoading}
          dataSource={orgUsersRows}
          columns={[
            { title: "账号", dataIndex: "username", width: 120, ellipsis: true },
            { title: "姓名", width: 100, ellipsis: true, render: (_, r) => r.displayName || "—" },
            { title: "手机", dataIndex: "phone", width: 120, render: (t) => t || "—" },
            { title: "所属机构", ellipsis: true, render: (_, r) => nameById.get(String(r.orgId || "")) || r.orgId || "—" },
            { title: "状态", dataIndex: "status", width: 88, render: (s) => renderStatusTag(s) },
            { title: "锁定", dataIndex: "lockStatus", width: 88, render: (s) => s || "—" },
          ]}
          pagination={{
            current: orgUsersPage,
            pageSize: orgUsersPageSize,
            total: orgUsersTotal,
            showSizeChanger: false,
            onChange: (p) => setOrgUsersPage(p),
          }}
          scroll={{ x: 640 }}
        />
      </Modal>
      <Modal
        title="人员机构调整"
        open={migrateOpen}
        onCancel={closeMigrateModal}
        footer={
          <Space>
            <Button onClick={closeMigrateModal}>关闭</Button>
            <Button
              type="primary"
              onClick={onMoveMembers}
              disabled={!sourceOrgId || !targetOrgId || selectedUserIds.length === 0}
            >
              批量迁移到目标机构
            </Button>
          </Space>
        }
        destroyOnClose
        width={720}
        styles={{
          body: {
            height: "min(520px, 70vh)",
            maxHeight: "70vh",
            overflow: "hidden",
            display: "flex",
            flexDirection: "column",
            boxSizing: "border-box",
          },
        }}
      >
        <div style={{ flexShrink: 0, fontSize: 12, color: token.colorTextSecondary, marginBottom: 12 }}>
          将所选用户从源机构批量改挂到目标机构（与用户管理中逐个修改所属机构效果相同）。
        </div>
        <Space wrap align="start" style={{ flexShrink: 0, marginBottom: 12 }}>
          <Form.Item label="源机构" style={{ marginBottom: 0 }}>
            <Select
              allowClear
              placeholder="请选择"
              style={{ width: 220 }}
              value={sourceOrgId || undefined}
              onChange={(v) => {
                setSourceOrgId(v || "");
                setMemberUsers([]);
                setSelectedUserIds([]);
              }}
              options={orgSelectOptions}
            />
          </Form.Item>
          <Button type="primary" onClick={loadMemberUsers} loading={memberLoading}>
            加载人员
          </Button>
          <Form.Item label="目标机构" style={{ marginBottom: 0 }}>
            <Select
              allowClear
              placeholder="请选择"
              style={{ width: 220 }}
              value={targetOrgId || undefined}
              onChange={(v) => setTargetOrgId(v || "")}
              options={orgSelectOptions}
            />
          </Form.Item>
        </Space>
        <div style={{ flex: 1, minHeight: 0, height: 0, overflow: "hidden" }}>
          <Table
            size="middle"
            rowKey="userId"
            loading={memberLoading}
            columns={memberColumns}
            dataSource={memberUsers}
            pagination={false}
            scroll={{ y: 280, x: "max-content" }}
            rowSelection={{
              selectedRowKeys: selectedUserIds,
              onChange: (keys) => setSelectedUserIds(keys),
            }}
          />
        </div>
      </Modal>
      <Modal title="新增机构" open={createOpen} onCancel={() => { setCreateOpen(false); createForm.resetFields(); }} onOk={onCreateSubmit} destroyOnClose width={520}>
        <Form form={createForm} layout="vertical" initialValues={{ sort: 1, status: "ENABLED" }}>
          <Form.Item name="name" label="机构名称" rules={[{ required: true, message: "请输入机构名称" }]}>
            <Input placeholder="机构名称" />
          </Form.Item>
          <Form.Item name="code" label="机构编码">
            <Input placeholder="可选，租户内唯一" />
          </Form.Item>
          <Form.Item name="parentOrgId" label="上级机构">
            <TreeSelect
              allowClear
              showSearch
              treeDefaultExpandAll
              placeholder="不选则为根"
              treeData={parentOrgTreeData}
              treeNodeFilterProp="title"
              style={{ width: "100%" }}
            />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber min={0} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ value: "ENABLED", label: "启用" }, { value: "DISABLED", label: "禁用" }]} />
          </Form.Item>
        </Form>
      </Modal>
      <Modal title="编辑机构" open={editOpen} onCancel={() => { setEditOpen(false); setEditOrgId(null); editForm.resetFields(); }} onOk={onEditSubmit} destroyOnClose width={520}>
        <Form form={editForm} layout="vertical">
          <Form.Item name="name" label="机构名称" rules={[{ required: true, message: "请输入机构名称" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="code" label="机构编码">
            <Input allowClear placeholder="可选" />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber min={0} style={{ width: "100%" }} />
          </Form.Item>
        </Form>
      </Modal>
      <Modal title="调整上级机构" open={moveOpen} onCancel={() => { setMoveOpen(false); setMoveOrgId(null); setMoveInitialParentId("__root__"); moveForm.resetFields(); }} onOk={onMoveSubmit} destroyOnClose width={480}>
        <Form form={moveForm} layout="vertical">
          <Form.Item name="newParentOrgId" label="新上级">
            <TreeSelect
              showSearch
              treeDefaultExpandAll
              placeholder="选择机构"
              treeData={moveParentTreeData}
              treeNodeFilterProp="title"
              style={{ width: "100%" }}
            />
          </Form.Item>
        </Form>
      </Modal>
    </PageShell>
  );
}

export default OrgPage;
