import { useState } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import {
  ArrowRight,
  Eye,
  EyeOff,
  LockKeyhole,
  Mail,
  PanelsTopLeft,
} from "lucide-react";
import Landing from "./Landing";
import { useAuth } from "../context/AuthContext";
import { parseApiError } from "../utils/apiError";
import "./Auth.css";
import "./AuthForm.css";

export default function Login() {
  const { status, isAuthenticated, login } = useAuth();
  const location = useLocation();

  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // The route guard (next step) sends people here with "state.from"; after login they return to that page.
  const redirectTo = location.state?.from?.pathname ?? "/dashboard";
  const notice = location.state?.notice;

  const fieldErrors = error?.fieldErrors ?? {};
  const showAlert = error && Object.keys(fieldErrors).length === 0;

  // Still checking for an old session: show nothing instead of flashing the form.
  if (status === "loading") {
    return null;
  }

  // Already logged in (or just logged in): leave this page.
  if (isAuthenticated) {
    return <Navigate to={redirectTo} replace />;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login({ email: email.trim(), password });
      // On success the page redirects by itself (see the <Navigate> above).
    } catch (err) {
      setError(parseApiError(err));
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-login-page">
      {/* "inert" keeps keyboard users from tabbing into the blurred page behind the card */}
      <div className="auth-landing-background" aria-hidden="true" inert>
        <Landing />
      </div>

      <div className="auth-login-overlay">
        <header className="auth-topbar">
          <Link to="/" className="auth-brand">
            <span className="auth-brand-icon">
              <PanelsTopLeft size={21} />
            </span>
            <span>TeamCanvas</span>
          </Link>

          <p>
            Don't have an account?{" "}
            <Link to="/register">Sign up</Link>
          </p>
        </header>

        <main className="auth-login-main">
          <section className="auth-card auth-login-card">
            <span className="auth-eyebrow">WELCOME BACK</span>

            <h1>Log in to your account</h1>
            <p className="auth-description">
              Continue to TeamCanvas and pick up where you left off.
            </p>

            {notice && <div className="auth-notice" role="status">{notice}</div>}
            {showAlert && <div className="auth-alert" role="alert">{error.message}</div>}

            <form onSubmit={handleSubmit} noValidate>
              <label htmlFor="login-email">Email address</label>
              <div className={`auth-input-wrap ${fieldErrors.email ? "has-error" : ""}`}>
                <Mail size={18} />
                <input
                  id="login-email"
                  type="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  autoComplete="email"
                  aria-invalid={Boolean(fieldErrors.email)}
                  aria-describedby={fieldErrors.email ? "login-email-error" : undefined}
                  required
                />
              </div>
              {fieldErrors.email && (
                <p className="auth-field-error" id="login-email-error">{fieldErrors.email}</p>
              )}

              <label htmlFor="login-password">Password</label>
              <div className={`auth-input-wrap ${fieldErrors.password ? "has-error" : ""}`}>
                <LockKeyhole size={18} />
                <input
                  id="login-password"
                  type={showPassword ? "text" : "password"}
                  placeholder="Enter your password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  autoComplete="current-password"
                  aria-invalid={Boolean(fieldErrors.password)}
                  aria-describedby={fieldErrors.password ? "login-password-error" : undefined}
                  required
                />
                <button
                  type="button"
                  className="auth-icon-button"
                  onClick={() => setShowPassword((value) => !value)}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {fieldErrors.password && (
                <p className="auth-field-error" id="login-password-error">{fieldErrors.password}</p>
              )}

              <button className="auth-submit" type="submit" disabled={submitting}>
                {submitting ? (
                  <>
                    <span className="auth-spinner" aria-hidden="true" /> Logging in…
                  </>
                ) : (
                  <>
                    Log in <ArrowRight size={18} />
                  </>
                )}
              </button>
            </form>

            <p className="auth-card-footer">
              Don't have an account? <Link to="/register">Sign up</Link>
            </p>
          </section>
        </main>
      </div>
    </div>
  );
}