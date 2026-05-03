import { Tag } from "antd";
import dayjs from "dayjs";

export function formatTs(v) {
  if (v == null || v === "") return "—";
  const n = typeof v === "number" ? v : Number(String(v).trim());
  if (!Number.isFinite(n) || n <= 0) {
    const d = dayjs(v);
    return d.isValid() ? d.format("YYYY-MM-DD HH:mm:ss") : "—";
  }
  const ms = n < 1e11 ? n * 1000 : n;
  return dayjs(ms).format("YYYY-MM-DD HH:mm:ss");
}

export function statusLabelText(status) {
  if (status === "ENABLED") return "启用";
  if (status === "DISABLED") return "禁用";
  return status || "—";
}

export function renderStatusTag(status) {
  if (status === "ENABLED") return <Tag color="success">启用</Tag>;
  if (status === "DISABLED") return <Tag color="warning">禁用</Tag>;
  return <Tag>{status || "—"}</Tag>;
}
