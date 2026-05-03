import http from "./http";

export const queryMenuTree = (params) => http.get("/tenant/menus/tree", { params });
export const reorderMenus = (payload) => http.post("/tenant/menus:reorder", payload);
export const createMenu = (payload) => http.post("/tenant/menus", payload);
export const updateMenu = (menuId, payload) => http.patch(`/tenant/menus/${menuId}`, payload);
export const moveMenu = (menuId, payload) => http.post(`/tenant/menus/${menuId}:move`, payload);
export const enableMenu = (menuId) => http.post(`/tenant/menus/${menuId}:enable`);
export const disableMenu = (menuId) => http.post(`/tenant/menus/${menuId}:disable`);
export const deleteMenu = (menuId) => http.delete(`/tenant/menus/${menuId}`);
export const restoreMenu = (menuId) => http.post(`/tenant/menus/${menuId}:restore`);
export const queryMyMenus = (params) => http.get("/tenant/me/menus", { params });

