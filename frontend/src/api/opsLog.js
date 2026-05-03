import http from "./http";

export const getOpsLogsConfig = () => http.get("/tenant/ops-logs/config");
