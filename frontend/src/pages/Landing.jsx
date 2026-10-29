import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  ArrowUpRight,
  Check,
  CheckCircle2,
  ChevronRight,
  Circle,
  FolderKanban,
  LayoutDashboard,
  Moon,
  PanelsTopLeft,
  ShieldCheck,
  Sun,
  Users,
  Zap,
  Menu,
  X,
} from "lucide-react";
import "./Landing.css";

const features = [
  {
    icon: FolderKanban,
    title: "Everything in one place",
    description:
      "Organize projects, tasks, deadlines, and priorities in a workspace built for clarity.",
    color: "violet",
  },
  {
    icon: Users,
    title: "Better teamwork",
    description:
      "Bring your people together with shared workspaces, clear ownership, and team updates.",
    color: "blue",
  },
  {
    icon: Zap,
    title: "Progress without the chaos",
    description:
      "Know what is moving, what needs attention, and what your team should focus on next.",
    color: "amber",
  },
];

const tasks = [
  { title: "Design landing page", label: "Design", tone: "violet", done: true },
  { title: "Build authentication API", label: "Development", tone: "blue", done: false },
  { title: "Review project requirements", label: "Planning", tone: "amber", done: false },
];

function Reveal({ children, className = "", delay = 0 }) {
  const elementRef = useRef(null);

  useEffect(() => {
    const element = elementRef.current;
    if (!element) return;

    if (
      !("IntersectionObserver" in window) ||
      window.matchMedia("(prefers-reduced-motion: reduce)").matches
    ) {
      element.classList.add("is-visible");
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          element.classList.add("is-visible");
          observer.unobserve(element);
        }
      },
      { threshold: 0.12, rootMargin: "0px 0px -35px 0px" }
    );

    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  return (
    <div
      ref={elementRef}
      className={`reveal ${className}`}
      style={{ "--reveal-delay": `${delay}ms` }}
    >
      {children}
    </div>
  );
}

function ProductPreview() {
  return (
    <div className="product-scene">
      <div className="scene-orb scene-orb-one" />
      <div className="scene-orb scene-orb-two" />

      <div className="product-window">
        <div className="product-window-bar">
          <div className="window-dots">
            <span />
            <span />
            <span />
          </div>
          <span className="window-address">app.teamcanvas.com</span>
          <div className="window-avatar">Y</div>
        </div>

        <div className="product-window-body">
          <aside className="product-sidebar">
            <div className="product-brand">
              <PanelsTopLeft size={16} />
              <strong>TeamCanvas</strong>
            </div>

            <span className="product-nav-label">WORKSPACE</span>

            <div className="product-nav-item selected">
              <LayoutDashboard size={14} />
              Overview
            </div>
            <div className="product-nav-item">
              <FolderKanban size={14} />
              Projects
            </div>
            <div className="product-nav-item">
              <CheckCircle2 size={14} />
              My tasks
            </div>
            <div className="product-nav-item">
              <Users size={14} />
              Members
            </div>

            <div className="sidebar-mini-card">
              <span className="sidebar-mini-icon">
                <Zap size={14} />
              </span>
              <strong>Keep moving.</strong>
              <span>One task at a time.</span>
            </div>
          </aside>

          <div className="product-main">
            <div className="product-main-top">
              <div>
                <span className="preview-kicker">MONDAY, OCTOBER 12</span>
                <h3>Good morning, Yash</h3>
                <p>Here's your team's progress at a glance.</p>
              </div>
              <button className="preview-add" type="button">
                + New task
              </button>
            </div>

            <div className="preview-metrics">
              <div className="preview-metric">
                <span>Active projects</span>
                <strong>08</strong>
                <small>Across workspace</small>
              </div>
              <div className="preview-metric">
                <span>Tasks completed</span>
                <strong>24</strong>
                <small className="metric-positive">Moving forward</small>
              </div>
            </div>

            <div className="preview-board-title">
              <strong>My team's tasks</strong>
              <span>This week ▾</span>
            </div>

            <div className="preview-task-list">
              {tasks.map((task) => (
                <div className="preview-task" key={task.title}>
                  {task.done ? (
                    <CheckCircle2 size={16} className="task-check" />
                  ) : (
                    <Circle size={16} className="task-circle" />
                  )}
                  <span className={task.done ? "task-done-text" : ""}>
                    {task.title}
                  </span>
                  <span className={`task-label ${task.tone}`}>
                    {task.label}
                  </span>
                </div>
              ))}
            </div>

            <div className="preview-bottom">
              <div className="preview-people">
                <span>Y</span>
                <span>A</span>
                <span>R</span>
                <small>+5 together</small>
              </div>
              <span className="preview-live">
                <span /> All caught up
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="floating-status floating-status-top">
        <span className="floating-status-icon">
          <Check size={16} />
        </span>
        <span>
          <strong>Task completed</strong>
          <small>Homepage wireframes</small>
        </span>
      </div>

      <div className="floating-status floating-status-bottom">
        <span className="floating-status-icon people-icon">
          <Users size={16} />
        </span>
        <span>
          <strong>Teamwork, in sync</strong>
          <small>Everyone knows what's next</small>
        </span>
      </div>
    </div>
  );
}

export default function Landing() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const mobileMenuRef = useRef(null);
  const mobileMenuButtonRef = useRef(null);

  // Lock background scroll when mobile menu is open
  useEffect(() => {
    document.body.style.overflow = mobileMenuOpen ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [mobileMenuOpen]);

  useEffect(() => {
    if (!mobileMenuOpen) return;

    const handleOutsideClick = (event) => {
      const clickedInsideMenu = mobileMenuRef.current?.contains(event.target);
      const clickedToggleButton = mobileMenuButtonRef.current?.contains(event.target);

      if (!clickedInsideMenu && !clickedToggleButton) {
        setMobileMenuOpen(false);
      }
    };

    const handleEscape = (event) => {
      if (event.key === "Escape") {
        setMobileMenuOpen(false);
      }
    };

    document.addEventListener("pointerdown", handleOutsideClick);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("pointerdown", handleOutsideClick);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [mobileMenuOpen]);

  const [darkMode, setDarkMode] = useState(() => {
    try {
      return localStorage.getItem("teamcanvas-theme") === "dark";
    } catch {
      return false;
    }
  });

  useEffect(() => {
    document.documentElement.style.colorScheme = darkMode ? "dark" : "light";
    try {
      localStorage.setItem("teamcanvas-theme", darkMode ? "dark" : "light");
    } catch {
      // Graceful fallback
    }
  }, [darkMode]);

  return (
    <div className={`landing ${darkMode ? "theme-dark" : "theme-light"}`}>
      <header className="landing-header">
        <Link
          to="/"
          className="landing-brand"
          aria-label="TeamCanvas home"
          onClick={() => setMobileMenuOpen(false)}
        >
          <span className="landing-brand-icon">
            <PanelsTopLeft size={20} />
          </span>
          <span>TeamCanvas</span>
        </Link>

        <nav className="landing-nav" aria-label="Main navigation">
          <a href="#features">Features</a>
          <a href="#how-it-works">How it works</a>
        </nav>

        <div className="landing-actions">
          <button
            className="theme-toggle"
            type="button"
            onClick={() => setDarkMode((prev) => !prev)}
            aria-label={darkMode ? "Switch to light mode" : "Switch to dark mode"}
            title={darkMode ? "Switch to light mode" : "Switch to dark mode"}
          >
            {darkMode ? <Sun size={18} /> : <Moon size={18} />}
          </button>

          <Link to="/login" className="landing-login desktop-login">
            Log in
          </Link>

          <Link to="/register" className="landing-cta header-cta desktop-cta">
            Get started <ArrowRight size={15} />
          </Link>

          <button
            ref={mobileMenuButtonRef}
            className="mobile-menu-toggle"
            type="button"
            aria-label={mobileMenuOpen ? "Close menu" : "Open menu"}
            aria-expanded={mobileMenuOpen}
            onClick={() => setMobileMenuOpen((open) => !open)}
          >
            {mobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>

        <div
          ref={mobileMenuRef}
          className={`mobile-navigation ${
            mobileMenuOpen ? "mobile-navigation-open" : ""
          }`}
          aria-hidden={!mobileMenuOpen}
        >
          <nav aria-label="Mobile navigation">
            <a href="#features" onClick={() => setMobileMenuOpen(false)}>
              <FolderKanban size={17} />
              Features
              <ChevronRight size={16} />
            </a>

            <a href="#how-it-works" onClick={() => setMobileMenuOpen(false)}>
              <Zap size={17} />
              How it works
              <ChevronRight size={16} />
            </a>

            <div className="mobile-menu-divider" />

            <Link to="/login" onClick={() => setMobileMenuOpen(false)}>
              Log in
            </Link>

            <Link
              to="/register"
              className="landing-cta mobile-get-started"
              onClick={() => setMobileMenuOpen(false)}
            >
              Get started for free <ArrowRight size={16} />
            </Link>
          </nav>

          <div className="mobile-menu-footer">
            <ShieldCheck size={16} />
            A clearer way to work together.
          </div>
        </div>
      </header>

      <main>
        <section className="landing-hero">
          <Reveal className="hero-copy">
            <div className="hero-badge">
              <span className="hero-badge-dot" />
              A calmer way to get work done
              <ChevronRight size={14} />
            </div>

            <h1>
              Make space for
              <br />
              <span>great work.</span>
            </h1>

            <p className="hero-description">
              Your projects deserve more than scattered messages and endless
              follow-ups. Bring tasks, teammates, and ideas together in one
              focused workspace.
            </p>

            <div className="hero-actions">
              <Link to="/register" className="landing-cta hero-primary">
                Get started for free <ArrowRight size={17} />
              </Link>
              <a href="#features" className="hero-secondary">
                Explore TeamCanvas <ArrowUpRight size={16} />
              </a>
            </div>

            <div className="hero-trust">
              <ShieldCheck size={16} />
              <span>Clear priorities. Shared progress. Better teamwork.</span>
            </div>
          </Reveal>

          <Reveal className="hero-visual" delay={120}>
            <ProductPreview />
          </Reveal>
        </section>

        <Reveal>
          <div className="value-strip">
            <span>One place for your team's best work</span>
            <div className="value-item">
              <CheckCircle2 size={16} /> Stay organized
            </div>
            <div className="value-item">
              <Users size={16} /> Work together
            </div>
            <div className="value-item">
              <Zap size={16} /> Keep momentum
            </div>
          </div>
        </Reveal>

        <section className="landing-features" id="features">
          <Reveal className="landing-section-heading">
            <span className="section-eyebrow">LESS CHAOS, MORE CLARITY</span>
            <h2>
              Everything in its place.
              <br />
              Everyone on the same page.
            </h2>
            <p>
              A thoughtful workspace that helps your team focus on the work
              that matters.
            </p>
          </Reveal>

          <div className="feature-grid">
            {features.map(({ icon: Icon, title, description, color }, index) => (
              <Reveal key={title} delay={index * 90}>
                <article className="feature-card">
                  <div className={`feature-icon ${color}`}>
                    <Icon size={22} />
                  </div>
                  <h3>{title}</h3>
                  <p>{description}</p>
                  <a href="#how-it-works" className="feature-link">
                    Discover more <ArrowRight size={15} />
                  </a>
                </article>
              </Reveal>
            ))}
          </div>
        </section>

        <section className="landing-how" id="how-it-works">
          <Reveal className="how-copy">
            <span className="section-eyebrow">SIMPLE BY DESIGN</span>
            <h2>
              From first idea
              <br />
              to finished work.
            </h2>
            <p>
              Get your team aligned without adding complexity. Start small,
              build momentum, and make progress visible.
            </p>
            <Link to="/register" className="landing-cta">
              Create your workspace <ArrowRight size={16} />
            </Link>
          </Reveal>

          <div className="how-list">
            <Reveal delay={0}>
              <article className="how-item">
                <span className="how-number">01</span>
                <div className="how-icon"><FolderKanban size={19} /></div>
                <div>
                  <h3>Bring your work together</h3>
                  <p>Create a shared home for projects and ideas.</p>
                </div>
              </article>
            </Reveal>
            <Reveal delay={80}>
              <article className="how-item">
                <span className="how-number">02</span>
                <div className="how-icon"><CheckCircle2 size={19} /></div>
                <div>
                  <h3>Make the next step clear</h3>
                  <p>Organize tasks, set priorities, and assign ownership.</p>
                </div>
              </article>
            </Reveal>
            <Reveal delay={160}>
              <article className="how-item">
                <span className="how-number">03</span>
                <div className="how-icon"><Users size={19} /></div>
                <div>
                  <h3>Move forward as a team</h3>
                  <p>Keep everyone connected to progress and outcomes.</p>
                </div>
              </article>
            </Reveal>
          </div>
        </section>

        <Reveal className="landing-cta-wrap">
          <section className="landing-final-cta">
            <div className="final-cta-mark">
              <PanelsTopLeft size={24} />
            </div>
            <div className="final-cta-copy">
              <span className="section-eyebrow">YOUR NEXT CHAPTER STARTS HERE</span>
              <h2>Good work happens together.</h2>
              <p>Give your ideas a home and your team a clearer way forward.</p>
            </div>
            <Link to="/register" className="landing-cta final-cta-button">
              Get started <ArrowRight size={16} />
            </Link>
          </section>
        </Reveal>
      </main>

      <footer className="landing-footer">
        <Link to="/" className="landing-brand footer-brand">
          <span className="landing-brand-icon">
            <PanelsTopLeft size={18} />
          </span>
          <span>TeamCanvas</span>
        </Link>
        <p>Make space for great work.</p>
        <div className="footer-links">
          <a href="#features">Features</a>
          <Link to="/login">Log in</Link>
          <span>© 2026 TeamCanvas</span>
        </div>
      </footer>
    </div>
  );
}