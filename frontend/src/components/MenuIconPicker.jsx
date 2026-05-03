import { useMemo, useState } from "react";
import { Button, Input, Modal, Row, Col, Space, theme } from "antd";
import { getMenuIcon, MENU_ICON_OPTIONS } from "../config/menu-icons";

export default function MenuIconPicker({ value, onChange }) {
  const { token } = theme.useToken();
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");
  const filtered = useMemo(() => {
    const s = q.trim().toLowerCase();
    if (!s) return MENU_ICON_OPTIONS;
    return MENU_ICON_OPTIONS.filter((o) => o.name.toLowerCase().includes(s));
  }, [q]);

  const hasValue = Boolean(value && String(value).trim());
  const preview = getMenuIcon(value);

  return (
    <Space.Compact style={{ width: "100%" }}>
      <div
        style={{
          flex: 1,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          minHeight: 32,
          padding: "0 8px",
          border: `1px solid ${token.colorBorder}`,
          borderRight: 0,
          borderRadius: `${token.borderRadius}px 0 0 ${token.borderRadius}px`,
          background: token.colorBgContainer,
          fontSize: 18,
          lineHeight: 1,
        }}
      >
        {preview || <span style={{ color: token.colorTextQuaternary }}>—</span>}
      </div>
      <Button type="default" onClick={() => { setQ(""); setOpen(true); }}>
        选择
      </Button>
      <Button onClick={() => onChange?.("")} disabled={!hasValue}>
        清空
      </Button>
      <Modal title="Icons" open={open} onCancel={() => setOpen(false)} footer={null} width={720} destroyOnClose>
        <Input allowClear placeholder="Filter by name…" value={q} onChange={(e) => setQ(e.target.value)} style={{ marginBottom: 12 }} />
        <Row gutter={[8, 8]} style={{ maxHeight: 420, overflowY: "auto" }}>
          {filtered.map((o) => (
            <Col key={o.name} xs={6} sm={4} md={3} lg={2}>
              <Button
                type="text"
                block
                title={o.name}
                onClick={() => {
                  onChange?.(o.name);
                  setOpen(false);
                }}
                style={{ height: 40, padding: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 20 }}
              >
                {getMenuIcon(o.name)}
              </Button>
            </Col>
          ))}
        </Row>
      </Modal>
    </Space.Compact>
  );
}
