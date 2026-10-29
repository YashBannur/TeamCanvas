import { useCallback, useEffect, useMemo, useState } from 'react';
import { Activity, ChevronLeft, ChevronRight, RotateCcw } from 'lucide-react';
import { activityService } from '../../services/activityService';
import { formatDateTime } from '../../utils/date';
import './ProjectActivity.css';

function readableAction(action) {
  const labels = {
    TASK_CREATED: 'created a task',
    TASK_UPDATED: 'updated a task',
    TASK_DELETED: 'deleted a task',
    TASK_MOVED: 'moved a task',
    TASK_STATUS_CHANGED: 'changed a task status',
    TASK_APPROVED: 'approved a task',
    TASK_ASSIGNED: 'changed a task assignment',
    TASK_PRIORITY_CHANGED: 'changed a task priority',
    TASK_DUE_DATE_CHANGED: 'changed a task due date',
    COMMENT_ADDED: 'added a comment',
    COMMENT_CREATED: 'added a comment',
    COMMENT_UPDATED: 'updated a comment',
    COMMENT_DELETED: 'deleted a comment',
    PROJECT_CREATED: 'created the project',
    PROJECT_UPDATED: 'updated the project',
    PROJECT_ARCHIVED: 'archived the project',
    PROJECT_RESTORED: 'restored the project'
  };

  return labels[action] || action?.toLowerCase().replaceAll('_', ' ') || 'updated something';
}

function taskIdForEvent(event) {
  const targetType = event.targetType?.toUpperCase();

  if (targetType === 'TASK') {
    return event.action === 'TASK_DELETED' ? null : event.targetId;
  }

  if (targetType === 'COMMENT') {
    const match = event.details?.match(/(?:^|\s)taskId=(\d+)/);
    return match ? Number(match[1]) : null;
  }

  return null;
}

function targetDescription(event) {
  const targetType = event.targetType?.toUpperCase();

  if (targetType === 'TASK') return 'a task';
  if (targetType === 'COMMENT') return '';

  return targetType ? targetType.toLowerCase() : '';
}

export default function ProjectActivity({
  projectId,
  members = [],
  onOpenActivity
}) {
  const [page, setPage] = useState(0);
  const [activityPage, setActivityPage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const memberNames = useMemo(() => {
    const names = new Map();

    members.forEach((member) => {
      if (member.userId != null && member.name) {
        names.set(String(member.userId), member.name);
      }
    });

    return names;
  }, [members]);

  const loadActivity = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const result = await activityService.getProjectActivity(projectId, {
        page,
        size: 20
      });
      setActivityPage(result);
    } catch (requestError) {
      setError(
        requestError?.message ||
          'Could not load project activity. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  }, [projectId, page]);

  useEffect(() => {
    loadActivity();
  }, [loadActivity]);

  const events = activityPage?.content || [];
  const totalPages = activityPage?.totalPages ?? 1;

  return (
    <section className="project-activity" aria-label="Project activity">
      <div className="project-activity__header">
        <div>
          <h2>Project activity</h2>
          <p>Recent changes made by workspace members.</p>
        </div>

        <button
          type="button"
          className="button-secondary project-activity__refresh"
          onClick={loadActivity}
          disabled={loading}
          aria-label="Refresh activity"
        >
          <RotateCcw size={15} />
          Refresh
        </button>
      </div>

      {loading && !activityPage ? (
        <div className="project-activity__message" role="status">
          Loading activity…
        </div>
      ) : error ? (
        <div
          className="project-activity__message project-activity__error"
          role="alert"
        >
          <span>{error}</span>
          <button
            type="button"
            className="button-secondary"
            onClick={loadActivity}
          >
            Try again
          </button>
        </div>
      ) : events.length === 0 ? (
        <div className="project-activity__empty">
          <Activity size={24} />
          <p>No activity has been recorded for this project yet.</p>
        </div>
      ) : (
        <>
          <ol className="project-activity__list">
            {events.map((event) => {
              const actorName =
                memberNames.get(String(event.actorId)) ||
                (event.actorId != null
                  ? `User ${event.actorId}`
                  : 'A team member');
              const target = targetDescription(event);
              const taskId = taskIdForEvent(event);
              const isComment = event.targetType?.toUpperCase() === 'COMMENT';

              // The taskId in comment details is only for navigation; don't show it.
              const visibleDetails = isComment ? '' : event.details;

              const content = (
                <>
                  <p className="project-activity__description">
                    <strong>{actorName}</strong> {readableAction(event.action)}
                    {target ? `: ${target}` : ''}
                  </p>

                  {visibleDetails && (
                    <p className="project-activity__details">
                      {visibleDetails}
                    </p>
                  )}

                  <time
                    className="project-activity__time"
                    dateTime={event.createdAt}
                  >
                    {formatDateTime(event.createdAt)}
                  </time>
                </>
              );

              return (
                <li
                  className="project-activity__item"
                  key={
                    event.id ??
                    `${event.actorId}-${event.createdAt}-${event.action}`
                  }
                >
                  <span className="project-activity__marker" aria-hidden="true">
                    <Activity size={15} />
                  </span>

                  <div className="project-activity__body">
                    {taskId != null ? (
                      <button
                        type="button"
                        className="project-activity__open"
                        onClick={() =>
                          onOpenActivity?.({
                            taskId: Number(taskId),
                            tab: isComment ? 'comments' : 'activity',
                            commentId: isComment ? event.targetId : null,
                            eventId: isComment ? null : event.id
                          })
                        }
                        aria-label={
                          isComment
                            ? `Open this comment on the task`
                            : `Open this task activity`
                        }
                      >
                        {content}
                      </button>
                    ) : (
                      content
                    )}
                  </div>
                </li>
              );
            })}
          </ol>

          <div className="project-activity__pagination">
            <button
              type="button"
              className="button-secondary"
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              disabled={page === 0 || loading}
            >
              <ChevronLeft size={16} />
              Previous
            </button>

            <span>
              Page {page + 1} of {Math.max(totalPages, 1)}
            </span>

            <button
              type="button"
              className="button-secondary"
              onClick={() => setPage((current) => current + 1)}
              disabled={page + 1 >= totalPages || loading}
            >
              Next
              <ChevronRight size={16} />
            </button>
          </div>
        </>
      )}
    </section>
  );
}