import http from "./http";

export const uploadFile = (file, bizHint) => {
  const form = new FormData();
  form.append("file", file);
  if (bizHint) {
    form.append("bizHint", bizHint);
  }
  return http.post("/tenant/files:upload", form);
};

export const getDownloadUrl = (fileId) => http.get(`/tenant/files/${fileId}:download`);

export const getPreviewUrl = (fileId) => http.get(`/tenant/files/${fileId}:preview`);
