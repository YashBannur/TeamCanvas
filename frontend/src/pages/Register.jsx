import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import {
  ArrowRight,
  Check,
  Eye,
  EyeOff,
  Folder,
  LockKeyhole,
  Mail,
  PanelsTopLeft,
  UserRound,
  Users,
  Zap,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { parseApiError } from "../utils/apiError";
import "./Auth.css";
import "./AuthForm.css";

export default function Register() {
  const { status, isAuthenticated, register, login } = useAuth();
  const navigate = useNavigate();

  const [showPassword, setShowPassword] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // These are exactly the rules the backend enforces (8-72 characters, a letter and a number).
  const requirements = [
    { label: "8 to 72 characters", valid: password.length >= 8 && password.length <= 72 },
    { label: "Includes a letter", valid: /[A-Za-z]/.test(password) },
    { label: "Includes a number", valid: /\d/.test(password) },
  ];

  const fieldErrors = error?.fieldErrors ?? {};
  const showAlert = error && Object.keys(fieldErrors).length === 0;

  if (status === "loading") {
    return null;
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (!requirements.every((item) => item.valid)) {
      setError({ message: "Please meet all the password requirements.", fieldErrors: {} });
      return;
    }

    setError(null);
    setSubmitting(true);

    try {
      await register({ name: name.trim(), email: email.trim(), password });
    } catch (err) {
      const parsed = parseApiError(err);
      if (parsed.status === 409) {
        parsed.fieldErrors = { email: parsed.message };
      }
      setError(parsed);
      setSubmitting(false);
      return;
    }

    // The backend returns no tokens when registering, so log in with the same credentials.
    try {
      await login({ email: email.trim(), password });
      // On success the page redirects by itself (see the <Navigate> above).
    } catch {
      navigate("/login", {
        replace: true,
        state: { notice: "Your account was created. Please log in." },
      });
    }
  }

  return (
    <div className="auth-register-page">
      <header className="auth-topbar auth-register-topbar">
        <Link to="/" className="auth-brand">
          <span className="auth-brand-icon">
            <PanelsTopLeft size={21} />
          </span>
          <span>TeamCanvas</span>
        </Link>

        <p>
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </header>

      <main className="auth-register-main">
        <section className="auth-card auth-register-card">
          <span className="auth-eyebrow">CREATE YOUR ACCOUNT</span>

          <h1>Get started with TeamCanvas</h1>
          <p className="auth-description">
            Set up your account and start building with your team.
          </p>

          {showAlert && <div className="auth-alert" role="alert">{error.message}</div>}

          <form onSubmit={handleSubmit} noValidate>
            <label htmlFor="register-name">Full name</label>
            <div className={`auth-input-wrap ${fieldErrors.name ? "has-error" : ""}`}>
              <UserRound size={18} />
              <input
                id="register-name"
                type="text"
                placeholder="Your full name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                autoComplete="name"
                maxLength={100}
                aria-invalid={Boolean(fieldErrors.name)}
                aria-describedby={fieldErrors.name ? "register-name-error" : undefined}
                required
              />
            </div>
            {fieldErrors.name && (
              <p className="auth-field-error" id="register-name-error">{fieldErrors.name}</p>
            )}

            <label htmlFor="register-email">Email address</label>
            <div className={`auth-input-wrap ${fieldErrors.email ? "has-error" : ""}`}>
              <Mail size={18} />
              <input
                id="register-email"
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                autoComplete="email"
                aria-invalid={Boolean(fieldErrors.email)}
                aria-describedby={fieldErrors.email ? "register-email-error" : undefined}
                required
              />
            </div>
            {fieldErrors.email && (
              <p className="auth-field-error" id="register-email-error">{fieldErrors.email}</p>
            )}

            <label htmlFor="register-password">Password</label>
            <div className={`auth-input-wrap ${fieldErrors.password ? "has-error" : ""}`}>
              <LockKeyhole size={18} />
              <input
                id="register-password"
                type={showPassword ? "text" : "password"}
                placeholder="Create a password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="new-password"
                maxLength={72}
                aria-invalid={Boolean(fieldErrors.password)}
                aria-describedby={fieldErrors.password ? "register-password-error" : undefined}
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
              <p className="auth-field-error" id="register-password-error">{fieldErrors.password}</p>
            )}

            <ul className="auth-password-rules">
              {requirements.map((item) => (
                <li key={item.label} className={item.valid ? "valid" : ""}>
                  <span className="auth-rule-icon">
                    {item.valid ? <Check size={12} /> : null}
                  </span>
                  {item.label}
                </li>
              ))}
            </ul>

            <button className="auth-submit" type="submit" disabled={submitting}>
              {submitting ? (
                <>
                  <span className="auth-spinner" aria-hidden="true" /> Creating account…
                </>
              ) : (
                <>
                  Create account <ArrowRight size={18} />
                </>
              )}
            </button>
          </form>
        </section>

        <aside className="auth-decoration" aria-hidden="true">
          <div className="auth-decor-orb auth-decor-orb-one" />
          <div className="auth-decor-orb auth-decor-orb-two" />

          <div className="auth-floating-card auth-task-card">
            <div><Check size={16} /></div>
            <span className="auth-task-line long" />
            <div><Check size={16} /></div>
            <span className="auth-task-line medium" />
            <span className="auth-task-line short" />
            <span className="auth-task-line medium blue" />
          </div>

          <div className="auth-floating-card auth-team-card">
            <span className="auth-avatar avatar-one">Y</span>
            <span className="auth-avatar avatar-two">A</span>
            <span className="auth-avatar avatar-three">R</span>
            <span className="auth-more-people">+3</span>
          </div>

          <div className="auth-floating-card auth-progress-card">
            <div className="auth-progress-heading">
              <Zap size={16} />
              <span>Team productivity</span>
            </div>
            <strong>Moving forward</strong>
            <div className="auth-chart-bars">
              <i />
              <i />
              <i />
              <i />
              <i />
              <i />
            </div>
          </div>

          <div className="auth-decor-benefits">
            <div>
              <span><Folder size={20} /></span>
              <strong>Everything in one place</strong>
              <p>Organize projects, tasks, and deadlines.</p>
            </div>
            <div>
              <span><Users size={20} /></span>
              <strong>Better teamwork</strong>
              <p>Bring your people together with clear ownership.</p>
            </div>
            <div>
              <span><Zap size={20} /></span>
              <strong>Progress without the chaos</strong>
              <p>Focus on what matters and keep moving forward.</p>
            </div>
          </div>
        </aside>
      </main>
    </div>
  );
}