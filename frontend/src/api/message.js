import http from "./http";

export const queryMessages = (params) => http.get("/tenant/me/messages", { params });

export const getMessage = (messageId) => http.get(`/tenant/me/messages/${messageId}`);

export const markMessageRead = (messageId) => http.post(`/tenant/me/messages/${messageId}:read`);

export const markAllMessagesRead = (payload) => http.post("/tenant/me/messages:readAll", payload || {});
