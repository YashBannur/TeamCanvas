import { useEffect, useState } from 'react';
import { Activity } from 'lucide-react';
import { activityService } from '../../services/activityService';
import { formatDateTime } from '../../utils/date';
import { parseApiError } from '../../utils/apiError';

function readableAction(action) {
  const labels = {
    TASK_CREATED: 'created this task',
    TASK_UPDATED: 'updated this task',
    TASK_ASSIGNED: 'changed the assignee',
    TASK_STATUS_CHANGED: 'changed the status',
    TASK_APPROVED: 'approved this task',
    TASK_PRIORITY_CHANGED: 'changed the priority',
    TASK_DUE_DATE_CHANGED: 'changed the due date',
    TASK_DELETED: 'deleted this task'
  };

  return labels[action] || action?.toLowerCase().replaceAll('_', ' ') || 'updated this task';
}

export default function TaskActivity({
  taskId,
  workspaceMembers = [],
  focusEventId
}) {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setError('');

      try {
        const page = await activityService.getTaskActivity(taskId);
        if (active) setEvents(page?.content || []);
      } catch (requestError) {
        if (active) {
          const parsed = parseApiError(requestError);
          setError(parsed.message || 'Could not load task activity.');
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    load();
    return () => {
      active = false;
    };
  }, [taskId]);

  useEffect(() => {
    if (!focusEventId || loading) return;

    const timer = window.setTimeout(() => {
      document
        .getElementById(`task-activity-${focusEventId}`)
        ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 50);

    return () => window.clearTimeout(timer);
  }, [focusEventId, loading, events]);

  if (loading) return <p role="status">Loading task activity…</p>;
  if (error) return <p role="alert">{error}</p>;
  if (events.length === 0) return <p>No activity has been recorded for this task yet.</p>;

  return (
    <ol className="task-activity-list" aria-label="Task activity">
      {events.map((event) => {
        const member = workspaceMembers.find(
          (item) => Number(item.userId) === Number(event.actorId)
        );
        const actorName = member?.name || `User ${event.actorId}`;

        return (
          <li
            id={`task-activity-${event.id}`}
            key={event.id}
            className="task-activity-item"
            style={{
              display: 'flex',
              gap: 10,
              padding: '12px 8px',
              borderBottom: '1px solid var(--border-color)',
              background:
                Number(event.id) === Number(focusEventId) ? '#f1efff' : 'transparent',
              borderRadius: 8
            }}
          >
            <Activity size={17} aria-hidden="true" />
            <div>
              <p style={{ margin: 0 }}>
                <strong>{actorName}</strong> {readableAction(event.action)}
              </p>
              {event.details && (
                <p style={{ margin: '4px 0 0', color: 'var(--text-secondary)' }}>
                  {event.details}
                </p>
              )}
              <time
                dateTime={event.createdAt}
                style={{
                  display: 'inline-block',
                  marginTop: 4,
                  color: 'var(--text-secondary)',
                  fontSize: '0.8rem'
                }}
              >
                {formatDateTime(event.createdAt)}
              </time>
            </div>
          </li>
        );
      })}
    </ol>
  );
}