import { useEffect, useState } from "react";
import { Button, Card, Checkbox, Form, Input, Space, message, theme } from "antd";
import {
  changeMyPassword,
  getMyProfile,
  getNotificationPreferences,
  patchMyProfile,
  putNotificationPreferences,
} from "../../api/profile";
import PageShell from "../../components/page/PageShell";
import useRuntimeConfigStore from "../../store/runtime-config";

function ProfilePage() {
  const { token } = theme.useToken();
  const [profile, setProfile] = useState(null);
  const [displayName, setDisplayName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [prefs, setPrefs] = useState({ inApp: true, email: false, sms: false });
  const pwdMinLen = Number(useRuntimeConfigStore((s) => s.values["security.password.minLength"]) || 8);

  const load = async () => {
    const res = await getMyProfile();
    const d = res.data?.data;
    setProfile(d);
    if (d) {
      setDisplayName(d.displayName || "");
      setPhone(d.phone || "");
      setEmail(d.email || "");
    }
    const np = await getNotificationPreferences();
    const p = np.data?.data?.preferences;
    if (p && Object.keys(p).length) {
      setPrefs({ inApp: !!p.inApp, email: !!p.email, sms: !!p.sms, quietHours: p.quietHours });
    }
  };

  useEffect(() => {
    load();
  }, []);

  const saveProfile = async () => {
    await patchMyProfile({ displayName, phone, email });
    message.success("已保存");
    load();
  };

  const savePassword = async () => {
    if (!newPassword || newPassword.length < pwdMinLen) {
      message.error(`新密码至少 ${pwdMinLen} 位`);
      return;
    }
    await changeMyPassword({ oldPassword, newPassword });
    setOldPassword("");
    setNewPassword("");
    message.success("已修改，请重新登录");
    localStorage.removeItem("accessToken");
    window.location.href = "/login";
  };

  const savePrefs = async () => {
    await putNotificationPreferences({ preferences: prefs });
    message.success("已保存");
    load();
  };

  return (
    <PageShell title="个人中心">
      {profile ? (
        <Card size="small" title="基本资料" style={{ marginBottom: 16 }}>
          <p style={{ color: token.colorTextSecondary }}>userId: {profile.userId}</p>
          <p style={{ color: token.colorTextSecondary }}>username: {profile.username}</p>
          <Form layout="vertical" style={{ maxWidth: 400 }}>
            <Form.Item label="显示名">
              <Input value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
            </Form.Item>
            <Form.Item label="手机">
              <Input value={phone} onChange={(e) => setPhone(e.target.value)} />
            </Form.Item>
            <Form.Item label="邮箱">
              <Input value={email} onChange={(e) => setEmail(e.target.value)} />
            </Form.Item>
            <Button type="primary" onClick={saveProfile}>
              保存资料
            </Button>
          </Form>
        </Card>
      ) : null}
      <Card size="small" title="修改密码" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Password placeholder="旧密码" value={oldPassword} onChange={(e) => setOldPassword(e.target.value)} style={{ width: 200 }} />
          <Input.Password placeholder={`新密码(≥${pwdMinLen})`} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} style={{ width: 200 }} />
          <Button onClick={savePassword}>提交</Button>
        </Space>
      </Card>
      <Card size="small" title="通知偏好">
        <Space wrap align="center">
          <Checkbox checked={prefs.inApp} onChange={(e) => setPrefs({ ...prefs, inApp: e.target.checked })}>
            站内
          </Checkbox>
          <Checkbox checked={prefs.email} onChange={(e) => setPrefs({ ...prefs, email: e.target.checked })}>
            邮件
          </Checkbox>
          <Checkbox checked={prefs.sms} onChange={(e) => setPrefs({ ...prefs, sms: e.target.checked })}>
            短信
          </Checkbox>
          <Button type="primary" onClick={savePrefs}>
            保存偏好
          </Button>
        </Space>
      </Card>
    </PageShell>
  );
}

export default ProfilePage;
