import { useEffect, useMemo, useState } from "react";
import { Button, Empty, Form, Input, InputNumber, Modal, Popover, Select, Space, Switch, Tag, Typography, message, theme } from "antd";
import { queryTenantConfigs, updateTenantConfig } from "../../api/config";
import PageShell from "../../components/page/PageShell";
import QueryBar from "../../components/page/QueryBar";
import CONFIG_META, { GROUP_ORDER } from "../../config/config-meta";
import useRuntimeConfigStore from "../../store/runtime-config";

function ConfigPage() {
  const { token } = theme.useToken();
  const [draftQuery, setDraftQuery] = useState({ key: "" });
  const [query, setQuery] = useState({ key: "" });
  const [listData, setListData] = useState([]);
  const [draftValues, setDraftValues] = useState({});
  const [changedMap, setChangedMap] = useState({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const patchRuntimeConfig = useRuntimeConfigStore((s) => s.patchValue);
  const refreshRuntimeConfig = useRuntimeConfigStore((s) => s.refresh);

  const loadList = async () => {
    setLoading(true);
    try {
      const res = await queryTenantConfigs({ page: 1, pageSize: 200, key: query.key || undefined });
      const data = res.data?.data || {};
      setListData(Array.isArray(data.items) ? data.items : []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query.key]);

  useEffect(() => {
    const initial = {};
    listData.forEach((row) => { initial[row.key] = row.value ?? ""; });
    setDraftValues(initial);
    setChangedMap({});
  }, [listData]);

  const onSearch = () => {
    setQuery({ ...draftQuery });
  };

  const onReset = () => {
    const next = { key: "" };
    setDraftQuery(next);
    setQuery(next);
  };

  const moduleMap = useMemo(() => {
    const map = {};
    listData.forEach((item) => {
      const meta = CONFIG_META[item.key] || {};
      const groupKey = meta.group || "other";
      const groupLabel = meta.groupLabel || "其他配置";
      if (!map[groupKey]) {
        map[groupKey] = {
          key: groupKey,
          label: groupLabel,
          items: [],
        };
      }
      map[groupKey].items.push(item);
    });
    return map;
  }, [listData]);

  const modules = useMemo(() => {
    const all = GROUP_ORDER
      .filter((k) => moduleMap[k])
      .map((k) => moduleMap[k])
      .concat(Object.keys(moduleMap).filter((k) => !GROUP_ORDER.includes(k)).map((k) => moduleMap[k]));
    const keyword = (query.key || "").trim();
    return all
      .map((group) => {
        const filtered = group.items.filter((it) => {
          const meta = CONFIG_META[it.key] || {};
          const text = `${it.key} ${meta.label || ""} ${meta.description || ""}`;
          return !keyword || text.includes(keyword);
        });
        return { ...group, items: filtered };
      })
      .filter((g) => g.items.length > 0);
  }, [moduleMap, query.key]);

  const changedKeys = useMemo(() => Object.keys(changedMap).filter((k) => changedMap[k]), [changedMap]);

  const saveSingle = async (row, value) => {
    await updateTenantConfig(row.key, { value, expectedVersion: row.version });
    patchRuntimeConfig(row.key, value);
  };

  const saveKeys = async (keys) => {
    if (!keys.length) {
      return;
    }
    setSaving(true);
    try {
      const rowMap = {};
      listData.forEach((r) => {
        rowMap[r.key] = r;
      });
      for (let i = 0; i < keys.length; i += 1) {
        const key = keys[i];
        const row = rowMap[key];
        if (!row) {
          continue;
        }
        await saveSingle(row, draftValues[key]);
      }
      message.success(`已保存 ${keys.length} 项配置`);
      await refreshRuntimeConfig();
      loadList();
    } catch (e) {
      const msg = e?.response?.data?.message || e?.message || "保存失败";
      if (String(msg).includes("CONFIG_VERSION_CONFLICT")) {
        message.error("配置已被他人更新，请刷新后重试");
      } else {
        message.error(msg);
      }
    } finally {
      setSaving(false);
    }
  };

  const onModuleSave = async (moduleItems) => {
    const keys = moduleItems.map((x) => x.key).filter((k) => changedMap[k]);
    await saveKeys(keys);
  };

  const setValue = (key, value, baseline) => {
    setDraftValues((prev) => ({ ...prev, [key]: value }));
    setChangedMap((prev) => ({ ...prev, [key]: String(value ?? "") !== String(baseline ?? "") }));
  };

  const onModuleResetDefault = (moduleItems) => {
    const nextValues = { ...draftValues };
    const nextChanged = { ...changedMap };
    moduleItems.forEach((row) => {
      const meta = CONFIG_META[row.key] || {};
      const defaultValue = meta.defaultValue ?? "";
      nextValues[row.key] = defaultValue;
      const baseline = row.value ?? "";
      nextChanged[row.key] = String(defaultValue) !== String(baseline);
    });
    setDraftValues(nextValues);
    setChangedMap(nextChanged);
  };

  const onDiscardAll = () => {
    const nextValues = {};
    listData.forEach((row) => {
      nextValues[row.key] = row.value ?? "";
    });
    setDraftValues(nextValues);
    setChangedMap({});
  };

  const rowMetaPopover = (row) => (
    <Space direction="vertical" size={2}>
      <Typography.Text type="secondary">Key：{row.key}</Typography.Text>
      <Typography.Text type="secondary">版本：{row.version ?? "-"}</Typography.Text>
      <Typography.Text type="secondary">更新人：{row.updatedBy || "-"}</Typography.Text>
      <Typography.Text type="secondary">更新时间：{row.updatedAt || "-"}</Typography.Text>
    </Space>
  );

  const renderField = (row) => {
    const meta = CONFIG_META[row.key] || {};
    const baseline = row.value ?? "";
    const value = draftValues[row.key] ?? "";
    if (meta.control === "boolean") {
      const checked = String(value).toLowerCase() === "true";
      return (
        <Switch
          checked={checked}
          onChange={(v) => setValue(row.key, v ? "true" : "false", baseline)}
          checkedChildren="开"
          unCheckedChildren="关"
        />
      );
    }
    if (meta.control === "number") {
      return (
        <InputNumber
          value={value === "" ? null : Number(value)}
          min={meta.min}
          max={meta.max}
          style={{ width: "100%" }}
          onChange={(v) => setValue(row.key, v == null ? "" : String(v), baseline)}
        />
      );
    }
    if (meta.control === "select") {
      return (
        <Select
          value={String(value)}
          options={meta.options || []}
          onChange={(v) => setValue(row.key, v, baseline)}
          style={{ width: "100%" }}
        />
      );
    }
    return (
      <Input.TextArea
        value={String(value ?? "")}
        autoSize={{ minRows: 1, maxRows: 3 }}
        onChange={(e) => setValue(row.key, e.target.value, baseline)}
      />
    );
  };

  return (
    <PageShell title="配置中心">
      <QueryBar onSearch={onSearch} onReset={onReset} loading={loading}>
        <Form.Item label="配置项搜索">
          <Input
            allowClear
            style={{ width: 260 }}
            value={draftQuery.key}
            onChange={(e) => setDraftQuery((prev) => ({ ...prev, key: e.target.value }))}
            placeholder="按中文名或 key 搜索"
          />
        </Form.Item>
      </QueryBar>
      {modules.length === 0 ? (
        <Empty description="暂无可展示配置项" />
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
          {modules.map((module) => (
            <div
              key={module.key}
              style={{
                borderRadius: 10,
                border: `1px solid ${token.colorBorderSecondary}`,
                overflow: "hidden",
                background: token.colorBgContainer,
              }}
            >
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  gap: 12,
                  flexWrap: "wrap",
                  padding: "12px 16px",
                  background: token.colorFillAlter,
                  borderBottom: `1px solid ${token.colorBorderSecondary}`,
                }}
              >
                <Typography.Text strong style={{ fontSize: 15 }}>{module.label}</Typography.Text>
                <Space>
                  <Button size="small" type="text" onClick={() => onModuleResetDefault(module.items)}>
                    恢复默认
                  </Button>
                  <Button
                    type="primary"
                    size="small"
                    loading={saving}
                    onClick={() => onModuleSave(module.items)}
                    disabled={!module.items.some((x) => changedMap[x.key])}
                  >
                    保存本组
                  </Button>
                </Space>
              </div>
              {module.items.map((row, idx) => {
                const meta = CONFIG_META[row.key] || {};
                const changed = Boolean(changedMap[row.key]);
                const isLast = idx === module.items.length - 1;
                return (
                  <div
                    key={row.key}
                    style={{
                      display: "flex",
                      flexWrap: "wrap",
                      gap: 16,
                      padding: "14px 16px",
                      borderBottom: isLast ? "none" : `1px solid ${token.colorSplit}`,
                      background: changed ? token.colorPrimaryBg : undefined,
                      borderLeft: changed ? `3px solid ${token.colorPrimary}` : "3px solid transparent",
                    }}
                  >
                    <div style={{ flex: "1 1 220px", minWidth: 0 }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
                        <Typography.Text strong>{meta.label || row.key}</Typography.Text>
                        {changed ? <Tag color="processing">已修改</Tag> : null}
                        <Popover title="技术信息" content={rowMetaPopover(row)} trigger="click">
                          <Typography.Link type="secondary" style={{ fontSize: 12 }}>技术信息</Typography.Link>
                        </Popover>
                      </div>
                      <Typography.Paragraph type="secondary" style={{ margin: "6px 0 0", fontSize: 13, marginBottom: 0 }}>
                        {meta.description || "暂无说明"}
                        {meta.impact ? ` · 影响：${meta.impact}` : ""}
                      </Typography.Paragraph>
                    </div>
                    <div style={{ flex: "0 1 280px", minWidth: 200, alignSelf: "center" }}>
                      {renderField(row)}
                    </div>
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      )}
      <div
        style={{
          marginTop: 16,
          position: "sticky",
          bottom: 8,
          zIndex: 2,
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          gap: 12,
          flexWrap: "wrap",
          padding: "12px 16px",
          borderRadius: 10,
          border: `1px solid ${token.colorBorderSecondary}`,
          background: token.colorBgContainer,
          boxShadow: token.boxShadowSecondary,
        }}
      >
        <Typography.Text>待提交变更：{changedKeys.length} 项</Typography.Text>
        <Space>
          <Button onClick={onDiscardAll} disabled={!changedKeys.length}>
            放弃修改
          </Button>
          <Button
            type="primary"
            loading={saving}
            onClick={() => {
              Modal.confirm({
                title: "确认保存全部变更？",
                content: `本次将更新 ${changedKeys.length} 项配置`,
                onOk: () => saveKeys(changedKeys),
              });
            }}
            disabled={!changedKeys.length}
          >
            统一保存
          </Button>
        </Space>
      </div>
    </PageShell>
  );
}

export default ConfigPage;
