import http from "./http";

export const createAnnouncement = (payload) => http.post("/platform/announcements", payload);

export const publishAnnouncement = (announcementId) => http.post(`/platform/announcements/${announcementId}:publish`);

export const revokeAnnouncement = (announcementId, reason) =>
  http.post(`/platform/announcements/${announcementId}:revoke`, { reason });

export const queryAnnouncements = (params) => http.get("/platform/announcements", { params });

export const getAnnouncement = (announcementId) => http.get(`/platform/announcements/${announcementId}`);
