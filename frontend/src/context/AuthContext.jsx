import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import * as authService from "../services/authService";
import { onSessionExpired } from "../services/tokenStore";

const AuthContext = createContext(null);

/**
 * Holds the logged-in user for the whole app.
 *   status: "loading"        -> still checking whether an old session can be restored (right after page load)
 *           "authenticated"  -> user is set
 *           "anonymous"      -> nobody is logged in
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState("loading");

  // On page load the in-memory access token is gone; the refresh cookie may still give us a session.
  useEffect(() => {
    let cancelled = false;
    authService.restoreSession().then((restoredUser) => {
      if (cancelled) {
        return;
      }
      setUser(restoredUser);
      setStatus(restoredUser ? "authenticated" : "anonymous");
    });
    return () => {
      cancelled = true;
    };
  }, []);

  // The API client reports "the refresh token is no longer valid": the session is over.
  useEffect(
    () =>
      onSessionExpired(() => {
        setUser(null);
        setStatus("anonymous");
      }),
    [],
  );

  const login = useCallback(async (credentials) => {
    const loggedInUser = await authService.login(credentials);
    setUser(loggedInUser);
    setStatus("authenticated");
    return loggedInUser;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } finally {
      setUser(null);
      setStatus("anonymous");
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      status,
      isAuthenticated: status === "authenticated",
      login,
      logout,
      register: authService.register,
    }),
    [user, status, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside <AuthProvider>");
  }
  return context;
}