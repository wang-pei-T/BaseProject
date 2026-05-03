import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button, Form, Input, message } from "antd";
import useAuthStore from "../../store/auth";
import LoginBranding from "./LoginBranding";
import LoginSplitLayout from "./LoginSplitLayout";
import LoginThemeProvider from "./LoginThemeProvider";
import styles from "./login.module.css";

export default function PlatformLoginPage() {
  const navigate = useNavigate();
  const doLogin = useAuthStore((state) => state.login);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const onSubmit = async (values) => {
    setLoading(true);
    try {
      await doLogin(
        {
          username: values.username,
          password: values.password,
          loginScope: "PLATFORM",
        },
        "jwt",
      );
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
      <Link to="/login">返回租户登录</Link>
    </div>
  );

  return (
    <LoginThemeProvider>
      <LoginSplitLayout branding={<LoginBranding />} footer={footer}>
        <div className={styles.card}>
          <div className={styles.stagger0}>
            <h2 className={styles.cardTitle}>平台管理登录</h2>
            <p className={styles.cardSubtitle}>运营与租户治理，仅限平台账号</p>
          </div>
          <div className={styles.stagger1}>
            <Form
              form={form}
              layout="vertical"
              onFinish={onSubmit}
              initialValues={{
                username: "platform_admin",
                password: "Admin@123456",
              }}
              requiredMark="optional"
            >
              <div className={styles.stagger2}>
                <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
                  <Input size="large" autoComplete="username" />
                </Form.Item>
                <Form.Item name="password" label="密码" rules={[{ required: true }]}>
                  <Input.Password size="large" autoComplete="current-password" />
                </Form.Item>
              </div>
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
