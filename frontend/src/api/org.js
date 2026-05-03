import http from "./http";

export const queryOrgTree = (params) => http.get("/tenant/orgs/tree", { params });
export const getOrg = (orgId, params) => http.get(`/tenant/orgs/${orgId}`, { params });
export const createOrg = (payload) => http.post("/tenant/orgs", payload);
export const updateOrg = (orgId, payload) => http.patch(`/tenant/orgs/${orgId}`, payload);
export const moveOrg = (orgId, payload) => http.post(`/tenant/orgs/${orgId}:move`, payload);
export const enableOrg = (orgId) => http.post(`/tenant/orgs/${orgId}:enable`);
export const disableOrg = (orgId) => http.post(`/tenant/orgs/${orgId}:disable`);
export const deleteOrg = (orgId) => http.delete(`/tenant/orgs/${orgId}`);
export const restoreOrg = (orgId) => http.post(`/tenant/orgs/${orgId}:restore`);
export const moveOrgUsers = (targetOrgId, userIds) =>
  http.post(`/tenant/orgs/${targetOrgId}/users:move`, { userIds });

