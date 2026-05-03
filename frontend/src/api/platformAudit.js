import http from "./http";

export const queryPlatformAudits = (params) => http.get("/platform/audits", { params });
