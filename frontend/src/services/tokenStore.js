// Keeps the short-lived access token in MEMORY only: never in localStorage or sessionStorage.
// Nothing is saved to disk, and closing or reloading the tab clears it. The session is restored on reload
// through the HttpOnly refresh cookie (see authService.restoreSession).

let accessToken = null;
const sessionExpiredListeners = new Set();

export function getAccessToken() {
  return accessToken;
}

export function setAccessToken(token) {
  accessToken = token;
}

export function clearAccessToken() {
  accessToken = null;
}

// The API client calls notifySessionExpired() when a token refresh fails.
// The auth context (step 4.2) subscribes with onSessionExpired() and sends the user to the login page.
export function onSessionExpired(listener) {
  sessionExpiredListeners.add(listener);
  return () => sessionExpiredListeners.delete(listener);
}

export function notifySessionExpired() {
  sessionExpiredListeners.forEach((listener) => listener());
}