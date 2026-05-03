import axios from "axios";
import appConfig from "../config/app-config";

const http = axios.create({
  baseURL: appConfig.apiBaseUrl,
  timeout: 10000,
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      let platform = false;
      try {
        const raw = localStorage.getItem("authUser");
        platform = raw && JSON.parse(raw).principalType === "PLATFORM_ACCOUNT";
      } catch {
        platform = false;
      }
      localStorage.removeItem("accessToken");
      localStorage.removeItem("tokenExpiresAt");
      localStorage.removeItem("authUser");
      window.location.href = platform ? "/platform/login" : "/login";
    }
    if (status === 403) {
      window.alert("无权限访问");
    }
    return Promise.reject(error);
  }
);

export default http;

