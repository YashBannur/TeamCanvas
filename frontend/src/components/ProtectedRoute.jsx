import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./AppLayout.css";

/**
 * Keeps logged-out visitors away from the app screens. This is a convenience for the user only:
 * the real protection is the backend, which rejects every request without a valid token.
 */
export default function ProtectedRoute() {
  const { status, isAuthenticated } = useAuth();
  const location = useLocation();

  // Still checking whether an old session can be restored: wait, do not redirect yet.
  if (status === "loading") {
    return (
      <div className="app-loading" role="status" aria-live="polite">
        Loading TeamCanvas…
      </div>
    );
  }

  if (!isAuthenticated) {
    // "from" lets the login page send the user back to the page they wanted.
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}