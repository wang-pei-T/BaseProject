import http from "./http";

export const queryTenantAudits = (params) => http.get("/tenant/audits", { params });
export const getTenantAudit = (auditId) => http.get(`/tenant/audits/${auditId}`);
export const exportTenantAuditsCsv = (params) =>
  http.get("/tenant/audits/export", { params, responseType: "blob" });
