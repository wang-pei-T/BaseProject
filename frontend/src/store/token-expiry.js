let expiryTimer = null;

export function scheduleAccessTokenExpiry(expiresInSeconds) {
  if (expiryTimer) {
    clearTimeout(expiryTimer);
    expiryTimer = null;
  }
  const sec = Math.max(1, Number(expiresInSeconds) || 1800);
  expiryTimer = setTimeout(() => {
    expiryTimer = null;
    localStorage.removeItem("accessToken");
    localStorage.removeItem("tokenExpiresAt");
    window.location.href = "/login";
  }, sec * 1000);
}

export function clearAccessTokenExpirySchedule() {
  if (expiryTimer) {
    clearTimeout(expiryTimer);
    expiryTimer = null;
  }
}
