import http from "./http";

export const queryPlatformLogs = (params) => http.get("/platform/logs", { params });
