import { create } from "zustand";
import { login as loginApi, logout as logoutApi, fetchMe as fetchMeApi } from "../api/auth";
import { clearAccessTokenExpirySchedule, scheduleAccessTokenExpiry } from "./token-expiry";

const AUTH_USER_KEY = "authUser";

function readStoredUser() {
  try {
    const raw = localStorage.getItem(AUTH_USER_KEY);
    if (!raw) return null;
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

const useAuthStore = create((set) => ({
  user: readStoredUser(),
  token: localStorage.getItem("accessToken") || "",
  async login(form, authMode) {
    const response = await loginApi(form, authMode);
    const payload = response.data?.data || {};
    const token = payload.accessToken || "";
    const exp = Number(payload.expiresIn);
    const sec = Number.isFinite(exp) && exp > 0 ? exp : 1800;
    localStorage.setItem("accessToken", token);
    localStorage.setItem("tokenExpiresAt", String(Date.now() + sec * 1000));
    scheduleAccessTokenExpiry(sec);
    const user = payload.user || null;
    if (user) {
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(AUTH_USER_KEY);
    }
    set({ token, user });
    return payload;
  },
  async logout() {
    try {
      await logoutApi();
    } finally {
      clearAccessTokenExpirySchedule();
      localStorage.removeItem("accessToken");
      localStorage.removeItem("tokenExpiresAt");
      localStorage.removeItem(AUTH_USER_KEY);
      set({ token: "", user: null });
    }
  },
  async refreshMe() {
    const token = localStorage.getItem("accessToken");
    if (!token) return null;
    try {
      const res = await fetchMeApi();
      const u = res.data?.data;
      if (u) {
        localStorage.setItem(AUTH_USER_KEY, JSON.stringify(u));
        set({ user: u });
      }
      return u;
    } catch {
      return null;
    }
  },
}));

export default useAuthStore;
