import { useCallback, useEffect, useState } from "react";
import { Button, Form, Input, InputNumber, Select, Space, Typography, message } from "antd";
import useAuthStore from "../../store/auth";
import { platformHasPerm } from "../../utils/platform-perm";
import { assistForceLogoutUser, assistPermissionTrace } from "../../api/platformAssist";
import { queryPlatformTenants } from "../../api/platformTenant";
import PageShell from "../../components/page/PageShell";

function PlatformAssistPage() {
  const user = useAuthStore((s) => s.user);
  const can = (c) => platformHasPerm(user, c);
  const [tenantOpts, setTenantOpts] = useState([]);
  const [traceResult, setTraceResult] = useState(null);
  const [loadingTrace, setLoadingTrace] = useState(false);
  const [form] = Form.useForm();

  const loadTenants = useCallback(async () => {
    const res = await queryPlatformTenants({ page: 1, pageSize: 200, status: "ENABLED" });
    const items = res.data?.data?.items || [];
    setTenantOpts(items.map((t) => ({ value: Number(t.tenantId), label: `${t.tenantName} (${t.tenantCode})` })));
  }, []);

  useEffect(() => {
    loadTenants();
  }, [loadTenants]);

  const onForceLogout = async () => {
    const tenantId = form.getFieldValue("tenantId");
    const userId = form.getFieldValue("userId");
    const reason = form.getFieldValue("reason");
    if (!tenantId || !userId || !reason?.trim()) {
      message.warning("请填写租户、用户与原因");
      return;
    }
    try {
      await assistForceLogoutUser(tenantId, userId, reason.trim());
      message.success("已强制下线");
    } catch (e) {
      message.error(String(e?.response?.data?.message || e?.message || "失败"));
    }
  };

  const onTrace = async () => {
    const tenantId = form.getFieldValue("tenantId");
    const userId = form.getFieldValue("userId");
    const reason = form.getFieldValue("reason");
    if (!tenantId || !userId || !reason?.trim()) {
      message.warning("请填写租户、用户与原因");
      return;
    }
    setLoadingTrace(true);
    setTraceResult(null);
    try {
      const permCode = form.getFieldValue("permissionCode");
      const res = await assistPermissionTrace(tenantId, userId, {
        reason: reason.trim(),
        permissionCode: permCode?.trim() || undefined,
      });
      setTraceResult(res.data?.data ?? res.data);
    } catch (e) {
      message.error(String(e?.response?.data?.message || e?.message || "失败"));
    } finally {
      setLoadingTrace(false);
    }
  };

  return (
    <PageShell title="跨租户协助">
      <Typography.Paragraph type="secondary">
        须填写原因；操作将写入平台审计。
      </Typography.Paragraph>
      <Form form={form} layout="vertical" style={{ maxWidth: 560 }}>
        <Form.Item name="tenantId" label="目标租户" rules={[{ required: true }]}>
          <Select showSearch optionFilterProp="label" options={tenantOpts} placeholder="选择租户" />
        </Form.Item>
        <Form.Item name="userId" label="租户内用户 ID" rules={[{ required: true }]}>
          <InputNumber style={{ width: "100%" }} min={1} placeholder="用户主键" />
        </Form.Item>
        <Form.Item name="reason" label="原因" rules={[{ required: true }]}>
          <Input.TextArea rows={3} placeholder="协助原因" />
        </Form.Item>
        <Form.Item name="permissionCode" label="权限点(追溯可选)">
          <Input placeholder="如 tenant.user.read" allowClear />
        </Form.Item>
        <Space wrap>
          {can("platform.assist.force_logout_user") ? (
            <Button type="primary" danger onClick={onForceLogout}>
              强制下线该用户
            </Button>
          ) : null}
          {can("platform.assist.permission_trace") ? (
            <Button loading={loadingTrace} onClick={onTrace}>
              权限追溯
            </Button>
          ) : null}
        </Space>
      </Form>
      {traceResult ? (
        <pre style={{ marginTop: 16, padding: 12, background: "#f5f5f5", fontSize: 12, overflow: "auto" }}>
          {JSON.stringify(traceResult, null, 2)}
        </pre>
      ) : null}
    </PageShell>
  );
}

export default PlatformAssistPage;
