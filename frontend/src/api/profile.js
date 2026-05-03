import http from "./http";

export const getMyProfile = () => http.get("/tenant/me/profile");
export const patchMyProfile = (payload) => http.patch("/tenant/me/profile", payload);
export const changeMyPassword = (payload) => http.post("/tenant/me/password:change", payload);
export const getNotificationPreferences = () => http.get("/tenant/me/notification-preferences");
export const putNotificationPreferences = (payload) => http.put("/tenant/me/notification-preferences", payload);
