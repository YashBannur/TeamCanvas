import { useEffect, useState } from 'react';
import { NavLink, Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, Briefcase, Mail, LogOut, KanbanSquare, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { invitationService } from '../services/invitationService';
import { Avatar } from './ui/Avatar';

export default function Sidebar({ isOpen = false, onClose = () => {} }) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [inviteCount, setInviteCount] = useState(0);

  useEffect(() => {
    let isSubscribed = true;
    invitationService
      .getMyInvitations()
      .then((invites) => {
        if (isSubscribed && Array.isArray(invites)) {
          setInviteCount(invites.length);
        }
      })
      .catch(() => {});

    return () => {
      isSubscribed = false;
    };
  }, [location.pathname]);

  const handleNavClick = () => {
    if (window.innerWidth <= 768) {
      onClose();
    }
  };

  return (
    <aside
      className={`app-sidebar ${isOpen ? 'drawer-open' : ''}`}
      aria-label="Main Navigation"
    >
      <div className="sidebar-brand">
        <div className="sidebar-brand-left">
          <div className="sidebar-logo">
            <KanbanSquare size={20} />
          </div>
          <span className="sidebar-title">TeamCanvas</span>
        </div>

        {isOpen && (
          <button
            type="button"
            className="icon-button"
            onClick={onClose}
            aria-label="Close menu"
          >
            <X size={18} />
          </button>
        )}
      </div>

      <div className="sidebar-content">
        <div>
          <div className="sidebar-section-title">Menu</div>
          <nav className="sidebar-nav">
            <NavLink
              to="/dashboard"
              className={({ isActive }) =>
                isActive ? 'sidebar-link active' : 'sidebar-link'
              }
              onClick={handleNavClick}
            >
              <LayoutDashboard size={18} />
              <span>Dashboard</span>
            </NavLink>

            <NavLink
              to="/workspaces"
              className={({ isActive }) =>
                isActive ? 'sidebar-link active' : 'sidebar-link'
              }
              onClick={handleNavClick}
            >
              <Briefcase size={18} />
              <span>Workspaces</span>
            </NavLink>

            <NavLink
              to="/invitations"
              className={({ isActive }) =>
                isActive ? 'sidebar-link active' : 'sidebar-link'
              }
              onClick={handleNavClick}
            >
              <Mail size={18} />
              <span>Invitations</span>
              {inviteCount > 0 && (
                <span className="sidebar-badge">{inviteCount}</span>
              )}
            </NavLink>
          </nav>
        </div>
      </div>

      <div className="sidebar-footer">
        <Link
          to="/profile"
          className="user-snippet"
          title="View profile"
          onClick={handleNavClick}
        >
          <Avatar name={user?.name || user?.email} size={34} />
          <div className="user-info">
            <span className="user-name">{user?.name || 'User'}</span>
            <span className="user-email">{user?.email}</span>
          </div>
        </Link>
        <button
          type="button"
          className="icon-button"
          onClick={logout}
          title="Sign out"
          aria-label="Sign out"
        >
          <LogOut size={18} />
        </button>
      </div>
    </aside>
  );
}