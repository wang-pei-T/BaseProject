import http from "./http";

export const queryPlatformAccounts = (params) => http.get("/platform/accounts", { params });
export const getPlatformAccount = (accountId) => http.get(`/platform/accounts/${accountId}`);
export const createPlatformAccount = (payload) => http.post("/platform/accounts", payload);
export const updatePlatformAccount = (accountId, payload) => http.patch(`/platform/accounts/${accountId}`, payload);
export const enablePlatformAccount = (accountId) => http.post(`/platform/accounts/${accountId}:enable`);
export const disablePlatformAccount = (accountId) => http.post(`/platform/accounts/${accountId}:disable`);
export const resetPlatformAccountPassword = (accountId, payload) =>
  http.post(`/platform/accounts/${accountId}:resetPassword`, payload || {});
export const assignPlatformAccountRoles = (accountId, payload) =>
  http.post(`/platform/accounts/${accountId}:assignRoles`, payload);
