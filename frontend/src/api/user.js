import http from "./http";

export const queryUsers = (params) => http.get("/tenant/users", { params });
export const getUser = (userId, params) => http.get(`/tenant/users/${userId}`, { params });
export const createUser = (payload) => http.post("/tenant/users", payload);
export const updateUser = (userId, payload) => http.patch(`/tenant/users/${userId}`, payload);
export const enableUser = (userId) => http.post(`/tenant/users/${userId}:enable`);
export const disableUser = (userId) => http.post(`/tenant/users/${userId}:disable`);
export const lockUser = (userId, payload) => http.post(`/tenant/users/${userId}:lock`, payload || {});
export const unlockUser = (userId) => http.post(`/tenant/users/${userId}:unlock`);
export const resetUserPassword = (userId, payload) => http.post(`/tenant/users/${userId}:resetPassword`, payload || {});
export const replaceUserRoles = (userId, roleIds) => http.post(`/tenant/users/${userId}/roles:replace`, { roleIds });
export const deleteUser = (userId) => http.delete(`/tenant/users/${userId}`);
export const restoreUser = (userId) => http.post(`/tenant/users/${userId}:restore`);
