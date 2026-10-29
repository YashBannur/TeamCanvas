import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Bell, Menu } from 'lucide-react';
import { notificationService } from '../services/notificationService';

const TITLES = [
  { prefix: '/dashboard', title: 'Dashboard' },
  { prefix: '/workspaces', title: 'Workspaces' },
  { prefix: '/projects', title: 'Projects' },
  { prefix: '/invitations', title: 'Invitations' },
  { prefix: '/notifications', title: 'Notifications' },
  { prefix: '/profile', title: 'Profile' }
];

const NOTIFICATIONS_CHANGED_EVENT = 'teamcanvas:notifications-changed';

export default function Topbar({ onOpenMobileMenu = () => {} }) {
  const location = useLocation();
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    let active = true;

    const loadUnreadCount = async () => {
      try {
        const result = await notificationService.getUnreadCount();
        if (active) {
          setUnreadCount(result.count ?? 0);
        }
      } catch {
        if (active) {
          setUnreadCount(0);
        }
      }
    };

    loadUnreadCount();
    window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, loadUnreadCount);

    return () => {
      active = false;
      window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, loadUnreadCount);
    };
  }, [location.pathname]);

  const match = TITLES.find((item) => location.pathname.startsWith(item.prefix));
  const pageTitle = match ? match.title : 'Dashboard';
  const badgeText = unreadCount > 99 ? '99+' : unreadCount;

  return (
    <header className="app-topbar">
      <div className="topbar-left">
        <button
          type="button"
          className="icon-button topbar-menu-btn"
          onClick={onOpenMobileMenu}
          aria-label="Open sidebar navigation"
        >
          <Menu size={20} />
        </button>

        <div className="topbar-breadcrumbs">
          <span>TeamCanvas</span>
          <span>&rsaquo;</span>
          <strong>{pageTitle}</strong>
        </div>
      </div>

      <div className="topbar-actions">
        <Link
          to="/notifications"
          className="notification-bell"
          aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ''}`}
          title="Notifications"
        >
          <Bell size={20} />
          {unreadCount > 0 && (
            <span className="notification-bell__badge" aria-hidden="true">
              {badgeText}
            </span>
          )}
        </Link>
      </div>
    </header>
  );
}