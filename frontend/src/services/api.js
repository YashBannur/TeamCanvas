
import axios from "axios";
import {
  clearAccessToken,
  getAccessToken,
  notifySessionExpired,
  setAccessToken,
} from "./tokenStore";

export const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

// withCredentials lets the browser send the HttpOnly refresh cookie (the backend CORS settings allow it).
const api = axios.create({
  baseURL: API_URL,
  withCredentials: true,
  headers: { Accept: "application/json" },
});

// A separate client for the refresh call, so it never passes through the interceptors below (no loops).
const refreshClient = axios.create({ baseURL: API_URL, withCredentials: true });

// These calls are never retried after a 401: a 401 there means "wrong credentials", not "expired token".
const AUTH_PATHS = ["/api/auth/login", "/api/auth/register", "/api/auth/refresh", "/api/auth/logout"];

// ---- every request carries the access token (if we have one)
api.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ---- refresh, "single flight": refresh tokens are single-use, so parallel refreshes would break each other.
// Everybody who needs a new token waits for the same request.
let refreshPromise = null;

export function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post("/api/auth/refresh")
      .then((response) => {
        setAccessToken(response.data.accessToken);
        return response.data;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

// ---- on a 401: refresh once, then retry the original request with the new token
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    const isAuthCall = AUTH_PATHS.some((path) => original?.url?.includes(path));

    if (status !== 401 || !original || original._retried || isAuthCall) {
      return Promise.reject(error);
    }

    original._retried = true;
    try {
      const data = await refreshAccessToken();
      original.headers.Authorization = `Bearer ${data.accessToken}`;
      return api(original);
    } catch {
      // The refresh token is missing, expired or revoked: the session is over.
      clearAccessToken();
      notifySessionExpired();
      return Promise.reject(error);
    }
  },
);

export default api;