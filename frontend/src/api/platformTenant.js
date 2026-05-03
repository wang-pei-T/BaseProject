import http from "./http";

export const queryPlatformTenants = (params) => http.get("/platform/tenants", { params });
export const getPlatformTenant = (tenantId, params) => http.get(`/platform/tenants/${tenantId}`, { params });
export const createPlatformTenant = (payload) => http.post("/platform/tenants", payload);
export const updatePlatformTenant = (tenantId, payload) => http.patch(`/platform/tenants/${tenantId}`, payload);
export const enablePlatformTenant = (tenantId) => http.post(`/platform/tenants/${tenantId}:enable`);
export const disablePlatformTenant = (tenantId) => http.post(`/platform/tenants/${tenantId}:disable`);
export const renewPlatformTenant = (tenantId, payload) => http.post(`/platform/tenants/${tenantId}:renew`, payload);
export const resetTenantAdminPassword = (tenantId, payload) =>
  http.post(`/platform/tenants/${tenantId}/admin:resetPassword`, payload || {});
export const forceLogoutTenantAdmin = (tenantId, payload) =>
  http.post(`/platform/tenants/${tenantId}/admin:forceLogout`, payload || {});
export const deletePlatformTenant = (tenantId) => http.delete(`/platform/tenants/${tenantId}`);
export const restorePlatformTenant = (tenantId) => http.post(`/platform/tenants/${tenantId}:restore`);
