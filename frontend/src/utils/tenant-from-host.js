const DEFAULT_RESERVED = ["www", "api", "platform"];

function parseReserved() {
  const raw = import.meta.env.VITE_TENANT_RESERVED_LABELS;
  if (typeof raw !== "string" || raw.trim() === "") {
    return DEFAULT_RESERVED;
  }
  return raw
    .split(",")
    .map((s) => s.trim().toLowerCase())
    .filter(Boolean);
}

export function getTenantBaseHost() {
  const v = import.meta.env.VITE_TENANT_BASE_HOST;
  return typeof v === "string" ? v.trim().toLowerCase() : "";
}

export function parseTenantCodeFromHostname(hostname) {
  const base = getTenantBaseHost();
  if (!hostname || !base) {
    return null;
  }
  const h = String(hostname).split(":")[0].trim().toLowerCase();
  const b = base.toLowerCase();
  if (h === b) {
    return null;
  }
  const suf = `.${b}`;
  if (!h.endsWith(suf)) {
    return null;
  }
  const label = h.slice(0, -suf.length);
  if (!label || label.includes(".")) {
    return null;
  }
  const reserved = new Set(parseReserved().map((x) => x.toLowerCase()));
  if (reserved.has(label)) {
    return null;
  }
  return label;
}
