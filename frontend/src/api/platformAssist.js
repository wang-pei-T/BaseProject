import http from "./http";

export const assistForceLogoutUser = (tenantId, userId, reason) =>
  http.post(`/platform/assist/tenants/${tenantId}/users/${userId}:forceLogout`, { reason });

export const assistPermissionTrace = (tenantId, userId, params) =>
  http.get(`/platform/assist/tenants/${tenantId}/users/${userId}/permissions:trace`, { params });
