import http from "./http";

export const queryDictTypes = (params) => http.get("/tenant/dicts/types", { params });
export const createDictType = (payload) => http.post("/tenant/dicts/types", payload);
export const updateDictType = (dictTypeId, payload) => http.patch(`/tenant/dicts/types/${dictTypeId}`, payload);
export const enableDictType = (dictTypeId) => http.post(`/tenant/dicts/types/${dictTypeId}:enable`);
export const disableDictType = (dictTypeId) => http.post(`/tenant/dicts/types/${dictTypeId}:disable`);
export const deleteDictType = (dictTypeId) => http.delete(`/tenant/dicts/types/${dictTypeId}`);
export const restoreDictType = (dictTypeId) => http.post(`/tenant/dicts/types/${dictTypeId}:restore`);

export const queryDictItems = (params) => http.get("/tenant/dicts/items", { params });
export const createDictItem = (dictTypeId, payload) => http.post(`/tenant/dicts/types/${dictTypeId}/items`, payload);
export const updateDictItem = (dictItemId, payload) => http.patch(`/tenant/dicts/items/${dictItemId}`, payload);
export const enableDictItem = (dictItemId) => http.post(`/tenant/dicts/items/${dictItemId}:enable`);
export const disableDictItem = (dictItemId) => http.post(`/tenant/dicts/items/${dictItemId}:disable`);
export const deleteDictItem = (dictItemId) => http.delete(`/tenant/dicts/items/${dictItemId}`);
export const restoreDictItem = (dictItemId) => http.post(`/tenant/dicts/items/${dictItemId}:restore`);
export const reorderDictItems = (payload) => http.post("/tenant/dicts/items:reorder", payload);
