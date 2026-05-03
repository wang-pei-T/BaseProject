import http from "./http";

export const queryPermissions = () => http.get("/tenant/permissions");
export const queryPermissionTraceByRole = (roleId) => http.get(`/tenant/permissions/trace/roles/${roleId}`);

