import http from "./http";

export const queryPlatformRoles = (params) => http.get("/platform/roles", { params });
export const getPlatformRole = (roleId) => http.get(`/platform/roles/${roleId}`);
export const createPlatformRole = (payload) => http.post("/platform/roles", payload);
export const updatePlatformRole = (roleId, payload) => http.patch(`/platform/roles/${roleId}`, payload);
export const enablePlatformRole = (roleId) => http.post(`/platform/roles/${roleId}:enable`);
export const disablePlatformRole = (roleId) => http.post(`/platform/roles/${roleId}:disable`);
export const deletePlatformRole = (roleId) => http.post(`/platform/roles/${roleId}:delete`);
export const replacePlatformRolePermissions = (roleId, payload) =>
  http.post(`/platform/roles/${roleId}:replacePermissions`, payload);
export const queryPlatformPermissions = (params) => http.get("/platform/permissions", { params });
