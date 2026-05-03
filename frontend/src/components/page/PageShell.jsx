import { Typography } from "antd";

export default function PageShell({ title, subTitle, extra, children }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      {(title || extra) && (
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 16, flexWrap: "wrap" }}>
          <div>
            {title ? <Typography.Title level={4} style={{ margin: 0 }}>{title}</Typography.Title> : null}
            {subTitle ? <Typography.Text type="secondary">{subTitle}</Typography.Text> : null}
          </div>
          {extra}
        </div>
      )}
      {children}
    </div>
  );
}
