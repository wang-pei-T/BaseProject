import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Card,
  Dropdown,
  Form,
  Input,
  InputNumber,
  Layout,
  Modal,
  Pagination,
  Select,
  Space,
  Table,
  message,
  theme,
} from "antd";
import {
  createDictItem,
  createDictType,
  deleteDictItem,
  deleteDictType,
  disableDictItem,
  disableDictType,
  enableDictItem,
  enableDictType,
  queryDictItems,
  queryDictTypes,
  reorderDictItems,
  restoreDictItem,
  restoreDictType,
  updateDictItem,
  updateDictType,
} from "../../api/dict";
import PageShell from "../../components/page/PageShell";
import QueryBar from "../../components/page/QueryBar";
import TableCard from "../../components/page/TableCard";
import useRuntimeConfigStore from "../../store/runtime-config";
import { exportSheet } from "../../utils/exportExcel";
import { renderStatusTag, statusLabelText } from "../../utils/tableDisplay";

const emptyDraft = () => ({ keyword: "", includeDeleted: "0" });

function errMsg(e) {
  return e?.response?.data?.message || e?.message || "操作失败";
}

const ACCENT = "#008c8c";
const SIDER_WIDTH = 384;

function DictPage() {
  const { token } = theme.useToken();
  const uiDark = String(useRuntimeConfigStore((s) => s.values["ui.theme.default"]) || "light").toLowerCase() === "dark";
  const pageSizeDefault = Number(useRuntimeConfigStore((s) => s.values["ui.page.defaultSize"]) || 20);
  const [types, setTypes] = useState([]);
  const [items, setItems] = useState([]);
  const [typeModalOpen, setTypeModalOpen] = useState(false);
  const [typeModalMode, setTypeModalMode] = useState("create");
  const [typeEditId, setTypeEditId] = useState("");
  const [itemModalOpen, setItemModalOpen] = useState(false);
  const [itemModalMode, setItemModalMode] = useState("create");
  const [itemEditId, setItemEditId] = useState("");
  const [typeForm] = Form.useForm();
  const [itemFormApi] = Form.useForm();
  const [itemForm, setItemForm] = useState({ dictTypeId: "" });
  const [draftQuery, setDraftQuery] = useState(emptyDraft);
  const [activeQuery, setActiveQuery] = useState(emptyDraft);
  const [loadingTypes, setLoadingTypes] = useState(false);
  const [reorderOpen, setReorderOpen] = useState(false);
  const [reorderRows, setReorderRows] = useState([]);
  const [typePagination, setTypePagination] = useState(() => ({
    page: 1,
    pageSize: Number(useRuntimeConfigStore.getState().values["ui.page.defaultSize"] || 20),
    total: 0,
  }));
  const [itemPagination, setItemPagination] = useState(() => ({
    page: 1,
    pageSize: Number(useRuntimeConfigStore.getState().values["ui.page.defaultSize"] || 20),
    total: 0,
  }));

  useEffect(() => {
    setTypePagination((p) => ({ ...p, page: 1, pageSize: pageSizeDefault }));
    setItemPagination((p) => ({ ...p, page: 1, pageSize: pageSizeDefault }));
  }, [pageSizeDefault]);

  const loadTypes = useCallback(async (page, pageSize) => {
    setLoadingTypes(true);
    try {
      const response = await queryDictTypes({
        includeDeleted: activeQuery.includeDeleted === "1",
        keyword: activeQuery.keyword || undefined,
        page,
        pageSize,
      });
      const data = response.data?.data || {};
      const list = data.items || [];
      setTypes(list);
      setTypePagination({
        page: data.page || page,
        pageSize: data.pageSize || pageSize,
        total: data.total || 0,
      });
      setItemForm((prev) => {
        if (!prev.dictTypeId && list.length > 0) {
          return { ...prev, dictTypeId: String(list[0].dictTypeId) };
        }
        if (prev.dictTypeId && !list.some((t) => String(t.dictTypeId) === String(prev.dictTypeId))) {
          return { ...prev, dictTypeId: list.length > 0 ? String(list[0].dictTypeId) : "" };
        }
        return prev;
      });
    } catch (e) {
      message.error(errMsg(e));
    } finally {
      setLoadingTypes(false);
    }
  }, [activeQuery.includeDeleted, activeQuery.keyword]);

  useEffect(() => {
    loadTypes(typePagination.page, typePagination.pageSize);
  }, [loadTypes, typePagination.page, typePagination.pageSize]);

  const loadItems = useCallback(
    async (dictTypeId, page, pageSize) => {
      if (!dictTypeId) {
        setItems([]);
        return;
      }
      try {
        const response = await queryDictItems({
          dictTypeId,
          includeDeleted: activeQuery.includeDeleted === "1",
          page,
          pageSize,
        });
        const data = response.data?.data || {};
        setItems(data.items || []);
        setItemPagination({
          page: data.page || page,
          pageSize: data.pageSize || pageSize,
          total: data.total || 0,
        });
      } catch (e) {
        message.error(errMsg(e));
      }
    },
    [activeQuery.includeDeleted],
  );

  useEffect(() => {
    loadItems(itemForm.dictTypeId, itemPagination.page, itemPagination.pageSize);
  }, [itemForm.dictTypeId, loadItems, itemPagination.page, itemPagination.pageSize]);

  const onSearch = () => {
    setActiveQuery({ ...draftQuery });
    setTypePagination((prev) => ({ ...prev, page: 1 }));
    setItemPagination((prev) => ({ ...prev, page: 1 }));
  };

  const resetQuery = () => {
    const q = emptyDraft();
    setDraftQuery(q);
    setActiveQuery(q);
    setTypePagination((prev) => ({ ...prev, page: 1 }));
    setItemPagination((prev) => ({ ...prev, page: 1 }));
  };

  const onTypeModalOk = async () => {
    const values = await typeForm.validateFields();
    if (typeModalMode === "create") {
      await createDictType(values);
      message.success("已创建类型");
    } else {
      await updateDictType(typeEditId, { name: values.name });
      message.success("已更新类型");
    }
    typeForm.resetFields();
    setTypeModalOpen(false);
    loadTypes(typePagination.page, typePagination.pageSize);
  };

  const onItemModalOk = async () => {
    const values = await itemFormApi.validateFields();
    if (!itemForm.dictTypeId) {
      message.warning("请先选择字典类型");
      return Promise.reject();
    }
    const labelText = values.label != null && String(values.label).trim() !== "" ? String(values.label).trim() : values.itemKey;
    const payload = {
      itemKey: values.itemKey,
      itemValue: values.itemValue,
      label: labelText,
      sort: values.sort ?? 0,
      status: values.status ?? "ENABLED",
    };
    if (itemModalMode === "create") {
      await createDictItem(itemForm.dictTypeId, payload);
      message.success("已创建字典项");
    } else {
      await updateDictItem(itemEditId, payload);
      message.success("已更新字典项");
    }
    itemFormApi.resetFields();
    setItemModalOpen(false);
    loadItems(itemForm.dictTypeId, itemPagination.page, itemPagination.pageSize);
  };

  const exportTypes = () => {
    const headers = ["类型编码", "类型名称", "状态"];
    const rows = types.map((t) => [t.code || "", t.name || "", statusLabelText(t.status)]);
    exportSheet("字典类型", "类型", headers, rows);
    message.success("已导出");
  };

  const exportItems = () => {
    const headers = ["排序", "字典值", "标签", "存储值", "状态"];
    const rows = items.map((it, i) => [
      it.sort ?? i + 1,
      it.itemKey || "",
      it.label || "",
      it.itemValue || "",
      statusLabelText(it.status),
    ]);
    exportSheet("字典项", "字典项", headers, rows);
    message.success("已导出");
  };

  const runReorder = async () => {
    await reorderDictItems({
      items: reorderRows.map((r) => ({ dictItemId: r.dictItemId, sort: Number(r.sort) })),
    });
    message.success("排序已保存");
    setReorderOpen(false);
    loadItems(itemForm.dictTypeId, itemPagination.page, itemPagination.pageSize);
  };

  const openReorder = () => {
    setReorderRows(
      items.map((r) => ({
        dictItemId: r.dictItemId,
        itemKey: r.itemKey,
        label: r.label,
        sort: r.sort ?? 0,
      })),
    );
    setReorderOpen(true);
  };

  const openTypeEdit = (type) => {
    setTypeModalMode("edit");
    setTypeEditId(type.dictTypeId);
    typeForm.setFieldsValue({ code: type.code, name: type.name });
    setTypeModalOpen(true);
  };

  const typeMoreMenu = (type) => {
    const id = type.dictTypeId;
    const deleted = type.deletedAt != null;
    const itemsMenu = [];
    if (!deleted) {
      itemsMenu.push({
        key: "del",
        label: "删除",
        danger: true,
      });
    } else if (activeQuery.includeDeleted === "1") {
      itemsMenu.push({ key: "restore", label: "恢复" });
    }
    return {
      items: itemsMenu,
      onClick: async ({ key }) => {
        if (key === "del") {
          Modal.confirm({
            title: "确认删除该字典类型？",
            onOk: async () => {
              try {
                await deleteDictType(id);
                message.success("已删除");
                loadTypes(typePagination.page, typePagination.pageSize);
              } catch (e) {
                message.error(errMsg(e));
              }
            },
          });
        }
        if (key === "restore") {
          try {
            await restoreDictType(id);
            message.success("已恢复");
            loadTypes(typePagination.page, typePagination.pageSize);
          } catch (e) {
            message.error(errMsg(e));
          }
        }
      },
    };
  };

  const itemColumns = useMemo(
    () => [
      { title: "排序", width: 72, dataIndex: "sort" },
      { title: "字典值", dataIndex: "itemKey", width: 120 },
      { title: "标签", dataIndex: "label", ellipsis: true },
      { title: "存储值", dataIndex: "itemValue", ellipsis: true },
      { title: "状态", dataIndex: "status", width: 100, render: (s) => renderStatusTag(s) },
      {
        title: "操作",
        width: 220,
        fixed: "right",
        render: (_, r) => {
          const deleted = r.deletedAt != null;
          return (
            <Space size={0} wrap>
              {!deleted ? (
                <>
                  <Button
                    type="link"
                    size="small"
                    onClick={() => {
                      setItemModalMode("edit");
                      setItemEditId(r.dictItemId);
                      itemFormApi.setFieldsValue({
                        itemKey: r.itemKey,
                        itemValue: r.itemValue,
                        label: r.label || r.itemKey,
                        sort: r.sort ?? 0,
                        status: r.status ?? "ENABLED",
                      });
                      setItemModalOpen(true);
                    }}
                  >
                    编辑
                  </Button>
                  {r.status === "DISABLED" ? (
                    <Button
                      type="link"
                      size="small"
                      onClick={async () => {
                        try {
                          await enableDictItem(r.dictItemId);
                          message.success("已启用");
                          loadItems(itemForm.dictTypeId, itemPagination.page, itemPagination.pageSize);
                        } catch (e) {
                          message.error(errMsg(e));
                        }
                      }}
                    >
                      启用
                    </Button>
                  ) : (
                    <Button
                      type="link"
                      size="small"
                      onClick={async () => {
                        try {
                          await disableDictItem(r.dictItemId);
                          message.success("已禁用");
                          loadItems(itemForm.dictTypeId, itemPagination.page, itemPagination.pageSize);
                        } catch (e) {
                          message.error(errMsg(e));
                        }
                      }}
                    >
                      禁用
                    </Button>
                  )}
                  <Dropdown
                    menu={{
                      items: [{ key: "del", label: "删除", danger: true }],
                      onClick: ({ key }) => {
                        if (key === "del") {
                          Modal.confirm({
                            title: "确认删除该字典项？",
                            onOk: async () => {
                              try {
                                await deleteDictItem(r.dictItemId);
                                message.success("已删除");
                                loadItems(itemForm.dictTypeId, itemPagination.page, itemPagination.pageSize);
                              } catch (e) {
                                message.error(errMsg(e));
                              }
                            },
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
                </>
              ) : activeQuery.includeDeleted === "1" ? (
                <Dropdown
                  menu={{
                    items: [{ key: "restore", label: "恢复" }],
                    onClick: async ({ key }) => {
                      if (key === "restore") {
                        try {
                          await restoreDictItem(r.dictItemId);
                          message.success("已恢复");
                          loadItems(itemForm.dictTypeId, itemPagination.page, itemPagination.pageSize);
                        } catch (e) {
                          message.error(errMsg(e));
                        }
                      }
                    },
                  }}
                  trigger={["click"]}
                >
                  <Button type="link" size="small">
                    更多
                  </Button>
                </Dropdown>
              ) : null}
            </Space>
          );
        },
      },
    ],
    [itemForm.dictTypeId, loadItems, activeQuery.includeDeleted, itemPagination.page, itemPagination.pageSize],
  );

  const selectedTypeName = types.find((t) => String(t.dictTypeId) === String(itemForm.dictTypeId))?.name;

  const cardStyles = {
    header: { minHeight: 48 },
    body: { padding: 10, flex: 1, minHeight: 480, maxHeight: "calc(100vh - 280px)", overflow: "auto" },
  };

  return (
    <PageShell title="字典管理">
      <QueryBar onSearch={onSearch} onReset={resetQuery} loading={loadingTypes}>
        <Form.Item label="关键词">
          <Input
            allowClear
            placeholder="类型名称或编码"
            value={draftQuery.keyword}
            onChange={(e) => setDraftQuery((p) => ({ ...p, keyword: e.target.value }))}
            style={{ width: 200 }}
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
      <Layout style={{ background: "transparent", minHeight: 400, gap: 16 }}>
        <Layout.Sider
          width={SIDER_WIDTH}
          theme={uiDark ? "dark" : "light"}
          style={{ background: "transparent", flex: `0 0 ${SIDER_WIDTH}px`, maxWidth: SIDER_WIDTH, minWidth: SIDER_WIDTH }}
        >
          <Card
            size="small"
            styles={cardStyles}
            style={{ height: "100%", display: "flex", flexDirection: "column" }}
            title="字典类型"
            extra={
              <Space wrap>
                <Button
                  type="primary"
                  onClick={() => {
                    setTypeModalMode("create");
                    typeForm.resetFields();
                    setTypeModalOpen(true);
                  }}
                >
                  新增类型
                </Button>
                <Button onClick={exportTypes}>导出 Excel</Button>
              </Space>
            }
          >
            <div>
              {types.map((type) => {
                const selected = String(itemForm.dictTypeId) === String(type.dictTypeId);
                return (
                  <div
                    key={type.dictTypeId}
                    style={{
                      display: "flex",
                      alignItems: "stretch",
                      marginBottom: 8,
                      borderRadius: 8,
                      border: selected ? `1px solid ${ACCENT}` : `1px solid ${token.colorBorderSecondary}`,
                      background: selected ? token.colorPrimaryBg : token.colorFillAlter,
                      overflow: "hidden",
                    }}
                  >
                    <div
                      role="presentation"
                      onClick={() => {
                        setItemPagination((prev) => ({ ...prev, page: 1 }));
                        setItemForm((prev) => ({ ...prev, dictTypeId: String(type.dictTypeId) }));
                      }}
                      style={{
                        flex: 1,
                        minWidth: 0,
                        padding: "10px 12px",
                        cursor: "pointer",
                        borderLeft: selected ? `3px solid ${ACCENT}` : "3px solid transparent",
                      }}
                    >
                      <div style={{ fontWeight: 600, color: token.colorText, lineHeight: 1.35 }}>{type.name}</div>
                      <div style={{ fontSize: 12, color: token.colorTextSecondary, marginTop: 4 }}>{type.code}</div>
                      <div style={{ marginTop: 6 }}>{renderStatusTag(type.status)}</div>
                    </div>
                    <div
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        alignItems: "stretch",
                        justifyContent: "center",
                        gap: 2,
                        padding: "6px 8px",
                        borderLeft: `1px solid ${token.colorBorderSecondary}`,
                        minWidth: 72,
                      }}
                      onClick={(e) => e.stopPropagation()}
                    >
                      {type.deletedAt == null ? (
                        <>
                          <Button type="link" size="small" style={{ padding: 0, height: "auto" }} onClick={() => openTypeEdit(type)}>
                            编辑
                          </Button>
                          {type.status === "DISABLED" ? (
                            <Button
                              type="link"
                              size="small"
                              style={{ padding: 0, height: "auto" }}
                              onClick={async () => {
                                try {
                                  await enableDictType(type.dictTypeId);
                                  message.success("已启用");
                                  loadTypes(typePagination.page, typePagination.pageSize);
                                } catch (e) {
                                  message.error(errMsg(e));
                                }
                              }}
                            >
                              启用
                            </Button>
                          ) : (
                            <Button
                              type="link"
                              size="small"
                              danger
                              style={{ padding: 0, height: "auto" }}
                              onClick={async () => {
                                try {
                                  await disableDictType(type.dictTypeId);
                                  message.success("已禁用");
                                  loadTypes(typePagination.page, typePagination.pageSize);
                                } catch (e) {
                                  message.error(errMsg(e));
                                }
                              }}
                            >
                              禁用
                            </Button>
                          )}
                          <Dropdown menu={typeMoreMenu(type)} trigger={["click"]} placement="bottomRight">
                            <Button type="link" size="small" style={{ padding: 0, height: "auto" }}>
                              更多
                            </Button>
                          </Dropdown>
                        </>
                      ) : activeQuery.includeDeleted === "1" ? (
                        <Dropdown menu={typeMoreMenu(type)} trigger={["click"]} placement="bottomRight">
                          <Button type="link" size="small" style={{ padding: 0, height: "auto" }}>
                            更多
                          </Button>
                        </Dropdown>
                      ) : null}
                    </div>
                  </div>
                );
              })}
            </div>
            <div style={{ marginTop: 8, display: "flex", justifyContent: "flex-end" }}>
              <Pagination
                size="small"
                current={typePagination.page}
                pageSize={typePagination.pageSize}
                total={typePagination.total}
                onChange={(page, pageSize) => loadTypes(page, pageSize)}
              />
            </div>
          </Card>
        </Layout.Sider>
        <Layout.Content style={{ minWidth: 0 }}>
          <TableCard
            title={
              <span>
                字典项
                {itemForm.dictTypeId && selectedTypeName ? (
                  <span style={{ marginLeft: 8, color: ACCENT, fontWeight: 400 }}>— {selectedTypeName}</span>
                ) : null}
              </span>
            }
            rowKey={(r) => r.dictItemId}
            columns={itemColumns}
            dataSource={items}
            pagination={{
              current: itemPagination.page,
              pageSize: itemPagination.pageSize,
              total: itemPagination.total,
              onChange: (page, pageSize) => loadItems(itemForm.dictTypeId, page, pageSize),
            }}
            scroll={{ x: 960 }}
            extra={
              <Space wrap>
                <Button
                  type="primary"
                  onClick={() => {
                    setItemModalMode("create");
                    itemFormApi.resetFields();
                    itemFormApi.setFieldsValue({ sort: 0, status: "ENABLED" });
                    setItemModalOpen(true);
                  }}
                  disabled={!itemForm.dictTypeId}
                >
                  新增字典项
                </Button>
                <Button onClick={openReorder} disabled={!items.length}>
                  调整排序
                </Button>
                <Button onClick={exportItems} disabled={!items.length}>
                  导出 Excel
                </Button>
              </Space>
            }
          />
        </Layout.Content>
      </Layout>
      <Modal
        title={typeModalMode === "create" ? "新增字典类型" : "编辑字典类型"}
        open={typeModalOpen}
        onCancel={() => {
          setTypeModalOpen(false);
          typeForm.resetFields();
        }}
        onOk={onTypeModalOk}
        destroyOnClose
        width={520}
      >
        <Form form={typeForm} layout="vertical">
          {typeModalMode === "create" ? (
            <Form.Item name="code" label="类型编码" rules={[{ required: true, message: "请输入类型编码" }]}>
              <Input placeholder="类型编码" />
            </Form.Item>
          ) : (
            <Form.Item name="code" label="类型编码">
              <Input disabled />
            </Form.Item>
          )}
          <Form.Item name="name" label="类型名称" rules={[{ required: true, message: "请输入类型名称" }]}>
            <Input placeholder="类型名称" />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title={itemModalMode === "create" ? "新增字典项" : "编辑字典项"}
        open={itemModalOpen}
        onCancel={() => {
          setItemModalOpen(false);
          itemFormApi.resetFields();
        }}
        onOk={onItemModalOk}
        destroyOnClose
        width={560}
      >
        <Form form={itemFormApi} layout="vertical">
          <Form.Item name="itemKey" label="字典值" rules={[{ required: true, message: "请输入字典值" }]}>
            <Input placeholder="业务键，同类型内唯一" />
          </Form.Item>
          <Form.Item name="label" label="标签（展示）">
            <Input placeholder="默认同字典值" />
          </Form.Item>
          <Form.Item name="itemValue" label="存储值" rules={[{ required: true, message: "请输入存储值" }]}>
            <Input placeholder="可与字典值相同" />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber min={0} max={999999} style={{ width: "100%" }} />
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
        title="调整字典项排序"
        open={reorderOpen}
        onCancel={() => setReorderOpen(false)}
        onOk={() =>
          runReorder().catch((e) => {
            message.error(errMsg(e));
            return Promise.reject(e);
          })
        }
        width={640}
        destroyOnClose
      >
        <Table
          size="small"
          rowKey="dictItemId"
          pagination={false}
          dataSource={reorderRows}
          columns={[
            { title: "字典值", dataIndex: "itemKey", width: 120 },
            { title: "标签", dataIndex: "label", ellipsis: true },
            {
              title: "排序",
              dataIndex: "sort",
              width: 120,
              render: (v, row) => (
                <InputNumber
                  min={0}
                  max={999999}
                  value={v}
                  onChange={(n) => {
                    setReorderRows((prev) =>
                      prev.map((x) => (x.dictItemId === row.dictItemId ? { ...x, sort: n ?? 0 } : x)),
                    );
                  }}
                />
              ),
            },
          ]}
        />
      </Modal>
    </PageShell>
  );
}

export default DictPage;
