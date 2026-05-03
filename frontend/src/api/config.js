import http from "./http";

export const queryPlatformConfigs = (params) => http.get("/platform/configs", { params });
export const updatePlatformConfig = (key, payload) => http.patch(`/platform/configs/${key}`, payload);

export const queryTenantConfigs = (params) => http.get("/tenant/configs", { params });
export const updateTenantConfig = (key, payload) => http.patch(`/tenant/configs/${key}`, payload);

export const queryTenantOverridableConfigs = (params) => http.get("/tenant/configs:overridable", { params });
export const overrideTenantConfig = (key, payload) => http.post(`/tenant/configs/${key}:override`, payload);
export const clearTenantOverride = (key) => http.post(`/tenant/configs/${key}:clearOverride`);

