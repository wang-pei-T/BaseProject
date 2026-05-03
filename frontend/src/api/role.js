import http from "./http";

export const queryRoles = (params) => http.get("/tenant/roles", { params });
export const getRole = (roleId, params) => http.get(`/tenant/roles/${roleId}`, { params });
export const createRole = (payload) => http.post("/tenant/roles", payload);
export const updateRole = (roleId, payload) => http.patch(`/tenant/roles/${roleId}`, payload);
export const enableRole = (roleId) => http.post(`/tenant/roles/${roleId}:enable`);
export const disableRole = (roleId) => http.post(`/tenant/roles/${roleId}:disable`);
export const deleteRole = (roleId) => http.delete(`/tenant/roles/${roleId}`);
export const restoreRole = (roleId) => http.post(`/tenant/roles/${roleId}:restore`);
export const replaceRolePermissions = (roleId, permissionCodes) =>
  http.post(`/tenant/roles/${roleId}/permissions:replace`, { permissionCodes });
export const queryRoleMenus = (roleId) => http.get(`/tenant/roles/${roleId}/menus`);
export const replaceRoleMenus = (roleId, menuIds) => http.post(`/tenant/roles/${roleId}/menus:replace`, { menuIds });

