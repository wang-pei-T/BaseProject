import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button, Form, Input, Tag, message } from "antd";
import { getTenantLoginOptions } from "../../api/auth";
import useAuthStore from "../../store/auth";
import { parseTenantCodeFromHostname } from "../../utils/tenant-from-host";
import LoginBranding from "./LoginBranding";
import LoginSplitLayout from "./LoginSplitLayout";
import LoginThemeProvider from "./LoginThemeProvider";
import styles from "./login.module.css";

export default function TenantLoginPage() {
  const navigate = useNavigate();
  const doLogin = useAuthStore((state) => state.login);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [captchaEnabled, setCaptchaEnabled] = useState(false);
  const tenantCodeWatch = Form.useWatch("tenantCode", form);

  const implicitTenant = useMemo(
    () => (typeof window !== "undefined" ? parseTenantCodeFromHostname(window.location.hostname) : null),
    [],
  );
  const showTenantField = !implicitTenant;

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const optArg =
          implicitTenant != null
            ? undefined
            : tenantCodeWatch == null || String(tenantCodeWatch).trim() === ""
              ? undefined
              : String(tenantCodeWatch).trim();
        const res = await getTenantLoginOptions(optArg);
        const d = res.data?.data || {};
        if (!cancelled) {
          setCaptchaEnabled(Boolean(d.authCaptchaEnabled));
        }
      } catch {
        if (!cancelled) {
          setCaptchaEnabled(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [tenantCodeWatch, implicitTenant]);

  useEffect(() => {
    if (implicitTenant) {
      form.setFieldsValue({ tenantCode: implicitTenant, username: "admin", loginScope: "TENANT" });
    } else {
      form.setFieldsValue({ tenantCode: "default", username: "admin", loginScope: "TENANT" });
    }
  }, [form, implicitTenant]);

  const onSubmit = async (values) => {
    setLoading(true);
    try {
      const payload = {
        username: values.username,
        password: values.password,
        captchaToken: values.captchaToken,
        loginScope: "TENANT",
      };
      if (!implicitTenant) {
        payload.tenantCode = values.tenantCode;
      }
      await doLogin(payload, "jwt");
      message.success("登录成功");
      navigate("/", { replace: true });
    } catch (e) {
      message.error(e?.response?.data?.message || "登录失败");
    } finally {
      setLoading(false);
    }
  };

  const footer = (
    <div className={styles.pageFooter}>
      <Link to="/platform/login">平台管理</Link>
    </div>
  );

  return (
    <LoginThemeProvider>
      <LoginSplitLayout branding={<LoginBranding />} footer={footer}>
        <div className={styles.card}>
          <div className={styles.stagger0}>
            <h2 className={styles.cardTitle}>租户登录</h2>
            <p className={styles.cardSubtitle}>使用租户账号进入业务系统</p>
          </div>
          <div className={styles.stagger1}>
            <Form
              form={form}
              layout="vertical"
              onFinish={onSubmit}
              initialValues={{
                tenantCode: "default",
                username: "admin",
                password: "Admin@123456",
                loginScope: "TENANT",
              }}
              requiredMark="optional"
            >
              {implicitTenant ? (
                <div className={styles.formHint}>
                  <span>当前租户（子域）</span>
                  <Tag color="cyan">{implicitTenant}</Tag>
                </div>
              ) : null}
              {showTenantField ? (
                <Form.Item name="tenantCode" label="租户编码" rules={[{ required: true }]}>
                  <Input size="large" autoComplete="organization" />
                </Form.Item>
              ) : null}
              <div className={styles.stagger2}>
                <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
                  <Input size="large" autoComplete="username" />
                </Form.Item>
                <Form.Item name="password" label="密码" rules={[{ required: true }]}>
                  <Input.Password size="large" autoComplete="current-password" />
                </Form.Item>
              </div>
              {captchaEnabled ? (
                <div className={styles.stagger3}>
                  <Form.Item name="captchaToken" label="验证码" rules={[{ required: true, message: "请输入验证码" }]}>
                    <Input size="large" placeholder="输入验证码（演示）" />
                  </Form.Item>
                </div>
              ) : null}
              <div className={styles.stagger3}>
                <Form.Item style={{ marginBottom: 0 }}>
                  <Button type="primary" htmlType="submit" block size="large" loading={loading}>
                    登录
                  </Button>
                </Form.Item>
              </div>
            </Form>
          </div>
        </div>
      </LoginSplitLayout>
    </LoginThemeProvider>
  );
}
