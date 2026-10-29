import { useEffect, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import { projectService } from '../../services/projectService';
import { ErrorState } from '../../components/ui/ErrorState';
import { Skeleton } from '../../components/ui/Skeleton';
import { parseApiError } from '../../utils/apiError';
import './ProjectAnalytics.css';

const STATUS_LABELS = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  IN_REVIEW: 'In review',
  DONE: 'Done'
};

const PRIORITY_LABELS = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  URGENT: 'Urgent'
};

function getLocalIsoDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function defaultFromDate() {
  const date = new Date();
  date.setDate(date.getDate() - 29);
  return getLocalIsoDate(date);
}

function defaultToDate() {
  return getLocalIsoDate(new Date());
}

function formatTrendDate(value) {
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric'
  }).format(date);
}

function toChartData(counts, labels) {
  return Object.entries(counts || {}).map(([key, count]) => ({
    name: labels[key] || key,
    count
  }));
}

function MetricCard({ label, value, tone }) {
  return (
    <article className={`analytics-metric analytics-metric--${tone}`}>
      <span className="analytics-metric__label">{label}</span>
      <strong className="analytics-metric__value">{value}</strong>
    </article>
  );
}

export default function ProjectAnalytics({ projectId }) {
  const [from, setFrom] = useState(defaultFromDate);
  const [to, setTo] = useState(defaultToDate);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [rangeError, setRangeError] = useState('');

  useEffect(() => {
    let active = true;

    async function loadAnalytics() {
      setLoading(true);
      setError(null);
      setRangeError('');

      if (from && to && from > to) {
        setRangeError('The start date must be on or before the end date.');
        setLoading(false);
        return;
      }

      try {
        const result = await projectService.getAnalytics(projectId, { from, to });
        if (active) {
          setAnalytics(result);
        }
      } catch (err) {
        if (active) {
          setError(parseApiError(err));
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadAnalytics();

    return () => {
      active = false;
    };
  }, [projectId, from, to, refreshKey]);

  const retry = () => setRefreshKey((value) => value + 1);

  if (loading && !analytics) {
    return (
      <div className="analytics-loading">
        <Skeleton height="110px" />
        <Skeleton height="300px" />
        <Skeleton height="300px" />
      </div>
    );
  }

  if (error && !analytics) {
    return (
      <ErrorState
        title="Failed to load analytics"
        message={error.message || 'Please try again.'}
        onRetry={retry}
      />
    );
  }

  if (rangeError) {
    return (
      <div className="analytics-dashboard">
        <DateRangeControls
          from={from}
          to={to}
          onFromChange={setFrom}
          onToChange={setTo}
        />
        <p className="analytics-range-error" role="alert">{rangeError}</p>
      </div>
    );
  }

  if (!analytics) return null;

  const statusData = toChartData(analytics.tasksByStatus, STATUS_LABELS);
  const priorityData = toChartData(analytics.tasksByPriority, PRIORITY_LABELS);
  const trendData = (analytics.completionTrend || []).map((point) => ({
    ...point,
    label: formatTrendDate(point.date)
  }));
  const workloadData = (analytics.workloadByAssignee || []).map((item) => ({
    name: item.assigneeName,
    taskCount: item.taskCount
  }));

  return (
    <section className="analytics-dashboard" aria-label="Project analytics">
      <DateRangeControls
        from={from}
        to={to}
        onFromChange={setFrom}
        onToChange={setTo}
      />

      {error && (
        <div className="analytics-inline-error" role="alert">
          <span>{error.message || 'Could not refresh analytics.'}</span>
          <button type="button" className="button-secondary" onClick={retry}>
            Retry
          </button>
        </div>
      )}

      <div className="analytics-metrics">
        <MetricCard label="Completed" value={analytics.completedTasks} tone="green" />
        <MetricCard label="Pending" value={analytics.pendingTasks} tone="blue" />
        <MetricCard label="Overdue" value={analytics.overdueTasks} tone="red" />
      </div>

      <div className="analytics-chart-grid">
        <ChartCard title="Tasks by status">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={statusData} margin={{ top: 8, right: 12, left: -16, bottom: 4 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="name" tick={{ fontSize: 12 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
              <Tooltip />
              <Bar dataKey="count" name="Tasks" fill="#6258e8" radius={[5, 5, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Tasks by priority">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={priorityData} margin={{ top: 8, right: 12, left: -16, bottom: 4 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="name" tick={{ fontSize: 12 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
              <Tooltip />
              <Bar dataKey="count" name="Tasks" fill="#0ea5e9" radius={[5, 5, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Completion trend">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={trendData} margin={{ top: 8, right: 16, left: -16, bottom: 4 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 11 }} minTickGap={16} />
              <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
              <Tooltip />
              <Line
                type="monotone"
                dataKey="completedTasks"
                name="Completed"
                stroke="#10b981"
                strokeWidth={3}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Workload by assignee">
          {workloadData.length === 0 ? (
            <p className="analytics-chart-empty">No tasks are assigned yet.</p>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={workloadData}
                layout="vertical"
                margin={{ top: 8, right: 16, left: 12, bottom: 4 }}
              >
                <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                <XAxis type="number" allowDecimals={false} tick={{ fontSize: 12 }} />
                <YAxis
                  type="category"
                  dataKey="name"
                  width={100}
                  tick={{ fontSize: 11 }}
                />
                <Tooltip />
                <Bar
                  dataKey="taskCount"
                  name="Tasks"
                  fill="#f59e0b"
                  radius={[0, 5, 5, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          )}
        </ChartCard>
      </div>
    </section>
  );
}

function DateRangeControls({ from, to, onFromChange, onToChange }) {
  return (
    <div className="analytics-date-range">
      <label>
        <span>Trend from</span>
        <input
          type="date"
          value={from}
          max={to || undefined}
          onChange={(event) => onFromChange(event.target.value)}
        />
      </label>
      <label>
        <span>Trend to</span>
        <input
          type="date"
          value={to}
          min={from || undefined}
          onChange={(event) => onToChange(event.target.value)}
        />
      </label>
    </div>
  );
}

function ChartCard({ title, children }) {
  return (
    <article className="analytics-chart-card">
      <h2>{title}</h2>
      <div className="analytics-chart-card__body">{children}</div>
    </article>
  );
}