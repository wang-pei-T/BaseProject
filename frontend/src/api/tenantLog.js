import http from "./http";

export const queryTenantLogs = (params) => http.get("/tenant/logs", { params });
export const exportTenantLogsCsv = (params) =>
  http.get("/tenant/logs/export", { params, responseType: "blob" });
