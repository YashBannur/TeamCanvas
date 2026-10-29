import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Mail, Check, X } from 'lucide-react';
import { invitationService } from '../../services/invitationService';
import { useToast } from '../../context/ToastContext';
import { useFetch } from '../../hooks/useFetch';
import { RoleBadge } from '../../components/ui/Badge';
import { Skeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { formatDateTime } from '../../utils/date';
import { parseApiError } from '../../utils/apiError';
import './Invitations.css';

export default function Invitations() {
  const navigate = useNavigate();
  const { success, error: showErrorToast } = useToast();
  const [processingId, setProcessingId] = useState(null);

  const fetchInvitations = useCallback(() => invitationService.getMyInvitations(), []);
  const { data: invitations, loading, error, reload } = useFetch(fetchInvitations);

  const handleAccept = async (invitation) => {
    setProcessingId(invitation.id);
    try {
      const workspace = await invitationService.acceptInvitation(invitation.id);
      success(`Joined workspace "${workspace.name}"`);
      navigate(`/workspaces/${workspace.id}`);
    } catch (err) {
      const parsed = parseApiError(err);
      if (parsed.status === 409) {
        showErrorToast('This invitation was already accepted, rejected, or cancelled.');
      } else if (parsed.status === 400) {
        showErrorToast('This invitation has expired.');
      } else {
        showErrorToast(parsed.message || 'Failed to accept invitation');
      }
      reload();
    } finally {
      setProcessingId(null);
    }
  };

  const handleReject = async (invitationId) => {
    setProcessingId(invitationId);
    try {
      await invitationService.rejectInvitation(invitationId);
      success('Invitation rejected');
      reload();
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to reject invitation');
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div className="invitations-page">
      <header className="invitations-header">
        <h1 className="invitations-title">Invitations Inbox</h1>
        <p className="invitations-subtitle">
          Manage workspace membership invitations sent to your account.
        </p>
      </header>

      {loading && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <Skeleton height="80px" />
          <Skeleton height="80px" />
        </div>
      )}

      {error && !loading && (
        <ErrorState
          title="Failed to load invitations"
          message={error.message}
          onRetry={reload}
        />
      )}

      {!loading && !error && invitations?.length === 0 && (
        <EmptyState
          icon={Mail}
          title="No pending invitations"
          description="When teammates invite you to their workspace, they will appear right here."
        />
      )}

      {!loading && !error && invitations?.length > 0 && (
        <div className="invitations-list">
          {invitations.map((inv) => (
            <div key={inv.id} className="invitation-card">
              <div className="invitation-card__info">
                <div className="invitation-card__title-row">
                  <h2 className="invitation-card__title">{inv.workspaceName}</h2>
                  <RoleBadge role={inv.role} />
                </div>
                <div className="invitation-card__meta">
                  <span>Invited by: <strong>{inv.invitedByName || 'Team Member'}</strong></span>
                  <span>Expires: {formatDateTime(inv.expiresAt)}</span>
                </div>
              </div>

              <div className="invitation-card__actions">
                <button
                  type="button"
                  className="button-secondary"
                  disabled={processingId === inv.id}
                  onClick={() => handleReject(inv.id)}
                >
                  <X size={16} />
                  Reject
                </button>
                <button
                  type="button"
                  className="button-primary"
                  disabled={processingId === inv.id}
                  onClick={() => handleAccept(inv)}
                >
                  <Check size={16} />
                  Accept &amp; Join
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}