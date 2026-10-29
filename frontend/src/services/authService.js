import api, { refreshAccessToken } from "./api";
import { clearAccessToken, setAccessToken } from "./tokenStore";

// Creates the account. The user must log in afterwards (the backend returns the new user, no tokens).
export async function register({ name, email, password }) {
  const { data } = await api.post("/api/auth/register", { name, email, password });
  return data;
}

// Keeps the access token in memory (the refresh token arrives as an HttpOnly cookie) and returns the user.
export async function login({ email, password }) {
  const { data } = await api.post("/api/auth/login", { email, password });
  setAccessToken(data.accessToken);
  return data.user;
}

// Called once when the app starts: the access token lives in memory and is gone after a page reload,
// but the refresh cookie may still be valid. Returns the user, or null if there is no valid session.
export async function restoreSession() {
  try {
    const data = await refreshAccessToken();
    return data.user;
  } catch {
    return null;
  }
}

// The server revokes the refresh token and clears the cookie. The local token is dropped either way.
export async function logout() {
  try {
    await api.post("/api/auth/logout");
  } finally {
    clearAccessToken();
  }
}

export async function fetchCurrentUser() {
  const { data } = await api.get("/api/me");
  return data;
}