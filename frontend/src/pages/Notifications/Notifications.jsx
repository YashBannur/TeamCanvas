import { useCallback, useEffect, useState } from 'react';
import { Bell, CheckCheck, Inbox } from 'lucide-react';
import { notificationService } from '../../services/notificationService';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { Skeleton } from '../../components/ui/Skeleton';
import { formatDateTime } from '../../utils/date';
import './Notifications.css';

const NOTIFICATIONS_CHANGED_EVENT = 'teamcanvas:notifications-changed';

const TYPE_LABELS = {
  TASK_ASSIGNED: 'Task assigned',
  COMMENT_ADDED: 'New comment',
  INVITATION_RECEIVED: 'Workspace invitation',
  PROJECT_ARCHIVED: 'Project archived'
};

export default function Notifications() {
  const [pageData, setPageData] = useState(null);
  const [page, setPage] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const reload = useCallback(() => {
    setRefreshKey((value) => value + 1);
  }, []);

  useEffect(() => {
    let active = true;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const result = await notificationService.getNotifications({
          page,
          size: 20,
          unreadOnly
        });
        if (active) {
          setPageData(result);
        }
      } catch (err) {
        if (active) {
          setError(err);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    load();

    return () => {
      active = false;
    };
  }, [page, unreadOnly, refreshKey]);

  const notifyCountChanged = () => {
    window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED_EVENT));
  };

  const handleMarkRead = async (notificationId) => {
    setBusyId(notificationId);
    try {
      await notificationService.markRead(notificationId);
      notifyCountChanged();
      reload();
    } catch (err) {
      setError(err);
    } finally {
      setBusyId(null);
    }
  };

  const handleMarkAllRead = async () => {
    setBusyId('all');
    try {
      await notificationService.markAllRead();
      notifyCountChanged();
      reload();
    } catch (err) {
      setError(err);
    } finally {
      setBusyId(null);
    }
  };

  const notifications = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;
  const currentPage = pageData?.page ?? page;

  return (
    <div className="notifications-page">
      <header className="notifications-header">
        <div>
          <h1 className="notifications-title">Notifications</h1>
          <p className="notifications-subtitle">
            Updates about tasks, comments, invitations, and projects.
          </p>
        </div>

        <button
          type="button"
          className="button-secondary"
          onClick={handleMarkAllRead}
          disabled={busyId !== null || loading || notifications.length === 0}
        >
          <CheckCheck size={16} />
          Mark all read
        </button>
      </header>

      <div className="notifications-toolbar">
        <label className="notifications-filter">
          <input
            type="checkbox"
            checked={unreadOnly}
            onChange={(event) => {
              setPage(0);
              setUnreadOnly(event.target.checked);
            }}
          />
          Unread only
        </label>
      </div>

      {loading && (
        <div className="notifications-skeletons">
          <Skeleton height="88px" />
          <Skeleton height="88px" />
          <Skeleton height="88px" />
        </div>
      )}

      {error && !loading && (
        <ErrorState
          title="Failed to load notifications"
          message={error.message || 'Please try again.'}
          onRetry={reload}
        />
      )}

      {!loading && !error && notifications.length === 0 && (
        <EmptyState
          icon={Inbox}
          title={unreadOnly ? 'No unread notifications' : 'You’re all caught up'}
          description={
            unreadOnly
              ? 'Unread notifications will appear here.'
              : 'New notifications will appear here.'
          }
        />
      )}

      {!loading && !error && notifications.length > 0 && (
        <>
          <div className="notifications-list">
            {notifications.map((notification) => (
              <article
                key={notification.id}
                className={`notification-card ${
                  notification.isRead ? '' : 'notification-card--unread'
                }`}
              >
                <div className="notification-card__icon" aria-hidden="true">
                  <Bell size={18} />
                </div>

                <div className="notification-card__content">
                  <div className="notification-card__heading">
                    <span className="notification-card__type">
                      {TYPE_LABELS[notification.type] || 'Notification'}
                    </span>
                    {!notification.isRead && (
                      <span className="notification-card__unread-label">Unread</span>
                    )}
                  </div>
                  <p className="notification-card__message">{notification.message}</p>
                  <time
                    className="notification-card__date"
                    dateTime={notification.createdAt}
                  >
                    {formatDateTime(notification.createdAt)}
                  </time>
                </div>

                {!notification.isRead && (
                  <button
                    type="button"
                    className="button-secondary notification-card__action"
                    disabled={busyId !== null}
                    onClick={() => handleMarkRead(notification.id)}
                  >
                    {busyId === notification.id ? 'Saving…' : 'Mark read'}
                  </button>
                )}
              </article>
            ))}
          </div>

          <nav className="notifications-pagination" aria-label="Notification pages">
            <button
              type="button"
              className="button-secondary"
              disabled={loading || currentPage <= 0}
              onClick={() => setPage((value) => Math.max(0, value - 1))}
            >
              Previous
            </button>
            <span>
              Page {currentPage + 1} of {Math.max(totalPages, 1)}
            </span>
            <button
              type="button"
              className="button-secondary"
              disabled={loading || currentPage + 1 >= totalPages}
              onClick={() => setPage((value) => value + 1)}
            >
              Next
            </button>
          </nav>
        </>
      )}
    </div>
  );
}