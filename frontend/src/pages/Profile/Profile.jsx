import { useState } from 'react';
import { LogOut, User, Calendar, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { Avatar } from '../../components/ui/Avatar';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { formatDate } from '../../utils/date';
import './Profile.css';

export default function Profile() {
  const { user, logout } = useAuth();
  const [isLogoutOpen, setIsLogoutOpen] = useState(false);

  return (
    <div className="profile-page">
      <header className="profile-header">
        <h1 className="profile-title">Account Profile</h1>
        <p className="profile-subtitle">
          Your personal TeamCanvas identity and account credentials.
        </p>
      </header>

      <div className="profile-card">
        <div className="profile-hero">
          <Avatar name={user?.name || user?.email} size={64} />
          <div className="profile-hero__info">
            <h2 className="profile-hero__name">{user?.name || 'User'}</h2>
            <p className="profile-hero__email">{user?.email}</p>
          </div>
        </div>

        <div className="profile-details-grid">
          <div className="profile-field">
            <span className="profile-field__label">Full Name</span>
            <div className="profile-field__value">
              {user?.name || '-'}
            </div>
          </div>

          <div className="profile-field">
            <span className="profile-field__label">Email Address</span>
            <div className="profile-field__value">
              {user?.email || '-'}
            </div>
          </div>

          <div className="profile-field">
            <span className="profile-field__label">User ID</span>
            <div className="profile-field__value">
              #{user?.id || '-'}
            </div>
          </div>

          <div className="profile-field">
            <span className="profile-field__label">Member Since</span>
            <div className="profile-field__value">
              {user?.createdAt ? formatDate(user.createdAt.split('T')[0]) : 'Recent'}
            </div>
          </div>
        </div>

        <div className="profile-actions">
          <button
            type="button"
            className="button-secondary"
            style={{ color: '#ef4444', borderColor: '#fee2e2' }}
            onClick={() => setIsLogoutOpen(true)}
          >
            <LogOut size={16} />
            Sign Out
          </button>
        </div>
      </div>

      <ConfirmDialog
        isOpen={isLogoutOpen}
        onClose={() => setIsLogoutOpen(false)}
        onConfirm={logout}
        title="Sign Out"
        message="Are you sure you want to end your current TeamCanvas session?"
        confirmText="Sign Out"
        isDestructive
      />
    </div>
  );
}