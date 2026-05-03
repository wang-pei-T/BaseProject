import http from "./http";

export const bindAttachment = (payload) => http.post("/tenant/attachments:bind", payload);

export const unbindAttachment = (attachmentId) => http.delete(`/tenant/attachments/${attachmentId}`);

export const deleteAttachment = (attachmentId) => http.post(`/tenant/attachments/${attachmentId}:delete`);

export const queryAttachments = (params) => http.get("/tenant/attachments", { params });
