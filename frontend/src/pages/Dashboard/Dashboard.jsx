import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Briefcase,
  FolderKanban,
  CheckCircle2,
  Clock,
  AlertCircle,
  Calendar,
  Plus,
  ArrowRight
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { dashboardService } from '../../services/dashboardService';
import { useFetch } from '../../hooks/useFetch';
import { PriorityBadge, StatusBadge } from '../../components/ui/Badge';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { CardSkeleton, Skeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { formatDate, formatDateTime, isOverdue } from '../../utils/date';
import './Dashboard.css';

function getGreeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
}

function getFormattedToday() {
  return new Intl.DateTimeFormat('en-US', {
    weekday: 'long',
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  }).format(new Date());
}

export default function Dashboard() {
  const { user } = useAuth();

  const fetchDashboard = useCallback(() => dashboardService.getDashboard(), []);
  const { data, loading, error, reload } = useFetch(fetchDashboard);

  if (loading) {
    return (
      <div className="dashboard-page">
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <Skeleton width="280px" height="34px" />
          <Skeleton width="220px" height="18px" />
        </div>

        <div className="stats-grid">
          <CardSkeleton />
          <CardSkeleton />
          <CardSkeleton />
          <CardSkeleton />
          <CardSkeleton />
        </div>

        <div className="dashboard-grid">
          <CardSkeleton />
          <CardSkeleton />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-page">
        <ErrorState
          title="Failed to load dashboard metrics"
          message={error.message || 'Unable to connect to service.'}
          onRetry={reload}
        />
      </div>
    );
  }

  const counts = data?.counts || {
    workspaces: 0,
    activeProjects: 0,
    openTasks: 0,
    dueSoon: 0,
    overdue: 0
  };

  const myTasks = data?.myTasks || [];
  const recentProjects = data?.recentProjects || [];
  const recentTasks = data?.recentTasks || [];

  // When caller has zero workspaces
  if (counts.workspaces === 0 && recentProjects.length === 0) {
    return (
      <div className="dashboard-page">
        <header className="dashboard-header">
          <div>
            <h1 className="dashboard-title">
              {getGreeting()}{user?.name ? `, ${user.name}` : ''}!
            </h1>
            <p className="dashboard-subtitle">{getFormattedToday()}</p>
          </div>
        </header>

        <EmptyState
          icon={Briefcase}
          title="No workspaces yet"
          description="Create your first team workspace to start adding projects, inviting collaborators, and tracking tasks."
          action={
            <Link to="/workspaces" className="button-primary">
              <Plus size={18} />
              Create your first workspace
            </Link>
          }
        />
      </div>
    );
  }

  return (
    <div className="dashboard-page">
      <header className="dashboard-header">
        <div>
          <h1 className="dashboard-title">
            {getGreeting()}{user?.name ? `, ${user.name}` : ''}!
          </h1>
          <p className="dashboard-subtitle">
            {getFormattedToday()} &bull; Here is your current overview.
          </p>
        </div>
      </header>

      {/* Metrics Row */}
      <section className="stats-grid" aria-label="Key workspace metrics">
        <div className="stat-card stat-card--primary">
          <span className="stat-card__label">Workspaces</span>
          <span className="stat-card__value">{counts.workspaces}</span>
        </div>

        <div className="stat-card">
          <span className="stat-card__label">Active Projects</span>
          <span className="stat-card__value">{counts.activeProjects}</span>
        </div>

        <div className="stat-card">
          <span className="stat-card__label">Open Tasks</span>
          <span className="stat-card__value">{counts.openTasks}</span>
        </div>

        <div className="stat-card stat-card--warning">
          <span className="stat-card__label">Due Soon</span>
          <span className="stat-card__value">{counts.dueSoon}</span>
        </div>

        <div className="stat-card stat-card--danger">
          <span className="stat-card__label">Overdue</span>
          <span className="stat-card__value">{counts.overdue}</span>
        </div>
      </section>

      {/* Two-Column Detail Grid */}
      <div className="dashboard-grid">
        {/* Left Column: My Tasks */}
        <div className="dashboard-panel">
          <div className="dashboard-panel__header">
            <h2 className="dashboard-panel__title">Assigned to Me</h2>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
              {myTasks.length} {myTasks.length === 1 ? 'task' : 'tasks'}
            </span>
          </div>

          <div className="dashboard-list">
            {myTasks.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', margin: 0 }}>
                You have no pending tasks assigned.
              </p>
            ) : (
              myTasks.map((t) => {
                const overdue = isOverdue(t.dueDate, t.status);
                return (
                  <Link
                    key={t.id}
                    to={`/projects/${t.projectId}`}
                    className="dashboard-task-item"
                  >
                    <div className="dashboard-task-item__content">
                      <h3 className="dashboard-task-item__title">{t.title}</h3>
                      <div className="dashboard-task-item__meta">
                        <span>{t.projectName}</span>
                        {t.dueDate && (
                          <span
                            className={`dashboard-task-item__meta-item ${
                              overdue ? 'is-overdue' : ''
                            }`}
                          >
                            <Calendar size={13} />
                            {formatDate(t.dueDate)} {overdue ? '(Overdue)' : ''}
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="dashboard-task-item__badges">
                      <PriorityBadge priority={t.priority} />
                      <StatusBadge status={t.status} />
                    </div>
                  </Link>
                );
              })
            )}
          </div>
        </div>

        {/* Right Column: Recent Projects */}
        <div className="dashboard-panel">
          <div className="dashboard-panel__header">
            <h2 className="dashboard-panel__title">Active Projects</h2>
            <Link
              to="/workspaces"
              style={{
                fontSize: '0.85rem',
                color: 'var(--primary-color)',
                textDecoration: 'none',
                display: 'flex',
                alignItems: 'center',
                gap: '4px'
              }}
            >
              All workspaces <ArrowRight size={14} />
            </Link>
          </div>

          <div className="dashboard-list">
            {recentProjects.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', margin: 0 }}>
                No active projects found.
              </p>
            ) : (
              recentProjects.map((p) => (
                <Link
                  key={p.id}
                  to={`/projects/${p.id}`}
                  className="dashboard-project-item"
                >
                  <div className="dashboard-project-item__header">
                    <h3 className="dashboard-project-item__title">{p.name}</h3>
                    <span style={{ fontSize: '0.8rem', fontWeight: 600 }}>
                      {p.progressPercent ?? 0}%
                    </span>
                  </div>

                  <ProgressBar value={p.progressPercent ?? 0} showLabel={false} />

                  <div className="dashboard-project-item__meta">
                    <span>
                      {p.completedTasks ?? 0} of {p.totalTasks ?? 0} tasks done
                    </span>
                    <span>
                      {p.dueDate ? `Due ${formatDate(p.dueDate)}` : 'No due date'}
                    </span>
                  </div>
                </Link>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Bottom Full-Width Section: Recent Task Updates */}
      {recentTasks.length > 0 && (
        <div className="dashboard-panel">
          <div className="dashboard-panel__header">
            <h2 className="dashboard-panel__title">Recent Activity</h2>
          </div>

          <div className="dashboard-list">
            {recentTasks.map((t) => (
              <Link
                key={t.id}
                to={`/projects/${t.projectId}`}
                className="dashboard-task-item"
              >
                <div className="dashboard-task-item__content">
                  <h3 className="dashboard-task-item__title">{t.title}</h3>
                  <div className="dashboard-task-item__meta">
                    <span>
                      {t.workspaceName} / {t.projectName}
                    </span>
                    <span>&bull;</span>
                    <span>Updated {formatDateTime(t.updatedAt)}</span>
                  </div>
                </div>

                <div className="dashboard-task-item__badges">
                  <StatusBadge status={t.status} />
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}