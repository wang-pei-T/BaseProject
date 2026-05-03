import { useCallback, useEffect, useState } from "react";
import { Alert, Button, Space, Spin, Typography } from "antd";
import { getOpsLogsConfig } from "../../api/opsLog";
import PageShell from "../../components/page/PageShell";
import useAuthStore from "../../store/auth";

function hasOpsLogPerm(user) {
  const p = user?.tenantPermissions;
  return Array.isArray(p) && p.includes("tenant.ops_log.read");
}

export default function RuntimeLogPage() {
  const user = useAuthStore((s) => s.user);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");
  const [cfg, setCfg] = useState(null);

  const load = useCallback(async () => {
    if (!hasOpsLogPerm(user)) {
      setErr("无 tenant.ops_log.read 权限");
      setCfg(null);
      return;
    }
    setLoading(true);
    setErr("");
    try {
      const res = await getOpsLogsConfig();
      const body = res.data;
      if (body && body.code !== 0) {
        setErr(body.message || "请求失败");
        setCfg(null);
        return;
      }
      setCfg(body?.data || null);
    } catch (e) {
      setErr(e?.response?.data?.message || String(e?.message || e));
      setCfg(null);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    load();
  }, [load]);

  const mode = cfg?.mode || "none";
  const externalUrl = cfg?.externalUrl || "";
  const lokiBaseUrl = cfg?.lokiBaseUrl || "";

  return (
    <PageShell title="运行日志">
      {err ? <Alert type="error" message={err} showIcon style={{ marginBottom: 16 }} /> : null}
      <Spin spinning={loading}>
        {mode === "none" ? (
          <Alert type="info" message="未配置集中日志（baseproject.ops-logs.mode），请联系管理员" showIcon />
        ) : null}
        {mode === "external" && externalUrl ? (
          <Space direction="vertical" size="middle" style={{ width: "100%" }}>
            <Typography.Text type="secondary">外链展示；若 iframe 被浏览器拦截，请使用新窗口打开。</Typography.Text>
            <Space>
              <Button type="primary" onClick={() => window.open(externalUrl, "_blank", "noopener,noreferrer")}>
                新窗口打开
              </Button>
            </Space>
            <iframe title="ops-logs" src={externalUrl} style={{ width: "100%", height: "70vh", border: "1px solid #f0f0f0" }} />
          </Space>
        ) : null}
        {mode === "external" && !externalUrl ? (
          <Alert type="warning" message="已选 external 模式但未配置 external-url" showIcon />
        ) : null}
        {mode === "loki" ? (
          <Space direction="vertical" size="small">
            <Typography.Text>
              Loki 基址（首期仅展示，复杂查询请用 Grafana 外链）：{lokiBaseUrl || "（未配置）"}
            </Typography.Text>
            {lokiBaseUrl ? (
              <Button type="link" onClick={() => window.open(lokiBaseUrl, "_blank", "noopener,noreferrer")}>
                打开 Loki / Grafana
              </Button>
            ) : null}
          </Space>
        ) : null}
      </Spin>
    </PageShell>
  );
}
