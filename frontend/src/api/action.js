import http from "./http";

export const queryActions = (params) => http.get("/tenant/actions", { params });
export const createAction = (payload) => http.post("/tenant/actions", payload);
export const updateAction = (actionId, payload) => http.patch(`/tenant/actions/${actionId}`, payload);
export const deleteAction = (actionId) => http.delete(`/tenant/actions/${actionId}`);

