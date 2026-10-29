import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./NotFound.css";

export default function NotFound() {
  const { isAuthenticated } = useAuth();

  return (
    <main className="not-found">
      <div className="not-found-card">
        <span className="not-found-code">404</span>
        <h1>Page not found</h1>
        <p>The page you are looking for does not exist or has been moved.</p>
        <Link className="not-found-link" to={isAuthenticated ? "/dashboard" : "/"}>
          {isAuthenticated ? "Back to dashboard" : "Back to home"}
        </Link>
      </div>
    </main>
  );
}