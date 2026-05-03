import http from "./http";

export const login = (payload, authMode = "jwt") =>
  http.post("/auth/login", payload, {
    headers: {
      "X-Auth-Mode": authMode,
    },
  });

export const getTenantLoginOptions = (tenantCode) => {
  const params = {};
  if (tenantCode != null && String(tenantCode).trim() !== "") {
    params.tenantCode = String(tenantCode).trim();
  }
  return http.get("/auth/tenant-login-options", { params });
};

export const logout = () => http.post("/auth/logout");

export const fetchMe = () => http.get("/auth/me");

export const getSessions = (params) => http.get("/auth/sessions", { params });

export const revokeSession = (sessionId) => http.delete(`/auth/sessions/${sessionId}`);

