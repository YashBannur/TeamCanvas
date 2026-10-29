import { useState, useCallback } from 'react';
import { Mail, Trash2, Send } from 'lucide-react';
import { workspaceService } from '../../services/workspaceService';
import { invitationService } from '../../services/invitationService';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useFetch } from '../../hooks/useFetch';
import { Avatar } from '../../components/ui/Avatar';
import { RoleBadge } from '../../components/ui/Badge';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { Skeleton } from '../../components/ui/Skeleton';
import { ErrorState } from '../../components/ui/ErrorState';
import { FormField } from '../../components/ui/FormField';
import { formatDate, formatDateTime } from '../../utils/date';
import { WORKSPACE_ROLES } from '../../constants/roles';
import {
  canChangeMemberRole,
  canRemoveMember,
  canInviteRole
} from '../../utils/permissions';
import { parseApiError } from '../../utils/apiError';

export default function MembersTab({ workspace }) {
  const { user } = useAuth();
  const { success, error: showErrorToast } = useToast();

  const fetchMembers = useCallback(
    () => workspaceService.getMembers(workspace.id),
    [workspace.id]
  );
  const {
    data: members,
    loading: membersLoading,
    error: membersError,
    reload: reloadMembers
  } = useFetch(fetchMembers);

  const isOwnerOrAdmin =
    workspace.myRole === WORKSPACE_ROLES.OWNER ||
    workspace.myRole === WORKSPACE_ROLES.ADMIN;

  const fetchInvitations = useCallback(() => {
    if (isOwnerOrAdmin) {
      return invitationService.getWorkspaceInvitations(workspace.id);
    }
    return Promise.resolve([]);
  }, [workspace.id, isOwnerOrAdmin]);

  const {
    data: invitations,
    loading: invitationsLoading,
    reload: reloadInvitations
  } = useFetch(fetchInvitations);

  // Invite form state
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState(WORKSPACE_ROLES.MEMBER);
  const [inviteErrors, setInviteErrors] = useState({});
  const [inviting, setInviting] = useState(false);

  // Removal confirm dialog state
  const [memberToRemove, setMemberToRemove] = useState(null);
  const [removingMember, setRemovingMember] = useState(false);

  // Cancel invitation confirm dialog state
  const [invitationToCancel, setInvitationToCancel] = useState(null);
  const [cancellingInvitation, setCancellingInvitation] = useState(false);

  const handleInvite = async (e) => {
    e.preventDefault();
    if (!inviteEmail.trim()) {
      setInviteErrors({ email: 'Email address is required' });
      return;
    }

    setInviting(true);
    setInviteErrors({});

    try {
      await invitationService.createInvitation(workspace.id, {
        email: inviteEmail.trim(),
        role: inviteRole
      });
      success(`Invitation sent to ${inviteEmail.trim()}`);
      setInviteEmail('');
      setInviteRole(WORKSPACE_ROLES.MEMBER);
      reloadInvitations();
    } catch (err) {
      const parsed = parseApiError(err);
      if (parsed.status === 404) {
        setInviteErrors({
          email: 'No registered user found with this email address'
        });
      } else if (parsed.status === 409) {
        showErrorToast(parsed.message || 'User is already a member or has a pending invitation');
      } else if (parsed.fieldErrors && Object.keys(parsed.fieldErrors).length > 0) {
        setInviteErrors(parsed.fieldErrors);
      } else {
        showErrorToast(parsed.message || 'Failed to send invitation');
      }
    } finally {
      setInviting(false);
    }
  };

  const handleRoleChange = async (targetUser, newRole) => {
    try {
      await workspaceService.updateMemberRole(workspace.id, targetUser.userId, newRole);
      success(`Updated ${targetUser.name}'s role to ${newRole}`);
      reloadMembers();
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to update member role');
    }
  };

  const handleRemoveMemberConfirm = async () => {
    if (!memberToRemove) return;
    setRemovingMember(true);
    try {
      await workspaceService.removeMember(workspace.id, memberToRemove.userId);
      success(`Removed ${memberToRemove.name} from workspace`);
      setMemberToRemove(null);
      reloadMembers();
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to remove member');
    } finally {
      setRemovingMember(false);
    }
  };

  const handleCancelInvitationConfirm = async () => {
    if (!invitationToCancel) return;
    setCancellingInvitation(true);
    try {
      await invitationService.cancelInvitation(workspace.id, invitationToCancel.id);
      success(`Cancelled invitation for ${invitationToCancel.inviteeEmail}`);
      setInvitationToCancel(null);
      reloadInvitations();
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to cancel invitation');
    } finally {
      setCancellingInvitation(false);
    }
  };

  return (
    <div className="members-container">
      {/* Invite member section (Owner / Admin) */}
      {isOwnerOrAdmin && (
        <section className="members-section">
          <div className="members-section__header">
            <h3 className="members-section__title">Invite New Member</h3>
          </div>
          <form
            onSubmit={handleInvite}
            style={{ display: 'flex', gap: '16px', alignItems: 'flex-start', flexWrap: 'wrap' }}
          >
            <div style={{ flex: '1 1 300px' }}>
              <FormField
                id="invite-email"
                required
                hint="Invite registered users by their exact TeamCanvas email."
                error={inviteErrors.email}
              >
                {(props) => (
                  <input
                    {...props}
                    type="email"
                    placeholder="teammate@example.com"
                    value={inviteEmail}
                    onChange={(e) => setInviteEmail(e.target.value)}
                    disabled={inviting}
                  />
                )}
              </FormField>
            </div>

            <div style={{ width: '160px' }}>
              <FormField id="invite-role">
                {(props) => (
                  <select
                    {...props}
                    value={inviteRole}
                    onChange={(e) => setInviteRole(e.target.value)}
                    disabled={inviting}
                  >
                    <option value={WORKSPACE_ROLES.MEMBER}>Member</option>
                    {canInviteRole(workspace.myRole, WORKSPACE_ROLES.ADMIN) && (
                      <option value={WORKSPACE_ROLES.ADMIN}>Admin</option>
                    )}
                  </select>
                )}
              </FormField>
            </div>

            <button
              type="submit"
              className="button-primary"
              disabled={inviting}
              style={{ marginTop: '2px', height: '42px' }}
            >
              <Send size={16} />
              {inviting ? 'Inviting...' : 'Send Invite'}
            </button>
          </form>
        </section>
      )}

      {/* Workspace members list */}
      <section className="members-section">
        <div className="members-section__header">
          <h3 className="members-section__title">
            Members ({members?.length || 0})
          </h3>
        </div>

        {membersLoading && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Skeleton height="40px" />
            <Skeleton height="40px" />
            <Skeleton height="40px" />
          </div>
        )}

        {membersError && (
          <ErrorState
            title="Failed to load members"
            message={membersError.message}
            onRetry={reloadMembers}
          />
        )}

        {!membersLoading && !membersError && members && (
          <div className="members-table-wrapper">
            <table className="members-table">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Role</th>
                  <th>Joined</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {members.map((m) => {
                  const isSelf = m.userId === user?.id;
                  const canChange =
                    canChangeMemberRole(workspace.myRole) &&
                    m.role !== WORKSPACE_ROLES.OWNER &&
                    !isSelf;
                  const canRemove = canRemoveMember(workspace.myRole, m.role, isSelf);

                  return (
                    <tr key={m.userId}>
                      <td>
                        <div className="member-cell-user">
                          <Avatar name={m.name} size={36} />
                          <div className="member-cell-info">
                            <span className="member-cell-name">
                              {m.name} {isSelf && '(You)'}
                            </span>
                            <span className="member-cell-email">{m.email}</span>
                          </div>
                        </div>
                      </td>
                      <td>
                        {canChange ? (
                          <select
                            value={m.role}
                            className="tc-form-field__select"
                            style={{ padding: '4px 8px', fontSize: '0.85rem', width: 'auto' }}
                            onChange={(e) => handleRoleChange(m, e.target.value)}
                          >
                            <option value={WORKSPACE_ROLES.MEMBER}>Member</option>
                            <option value={WORKSPACE_ROLES.ADMIN}>Admin</option>
                          </select>
                        ) : (
                          <RoleBadge role={m.role} />
                        )}
                      </td>
                      <td style={{ color: 'var(--text-secondary)' }}>
                        {formatDate(m.joinedAt?.split('T')[0]) || 'Recently'}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        {canRemove && (
                          <button
                            type="button"
                            className="icon-button"
                            style={{ color: '#ef4444' }}
                            onClick={() => setMemberToRemove(m)}
                            title={`Remove ${m.name}`}
                            aria-label={`Remove ${m.name}`}
                          >
                            <Trash2 size={16} />
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Pending Invitations list (Owner / Admin) */}
      {isOwnerOrAdmin && (
        <section className="members-section">
          <div className="members-section__header">
            <h3 className="members-section__title">
              Pending Invitations ({invitations?.length || 0})
            </h3>
          </div>

          {invitationsLoading && <Skeleton height="50px" />}

          {!invitationsLoading && invitations?.length === 0 && (
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', margin: 0 }}>
              No pending invitations for this workspace.
            </p>
          )}

          {!invitationsLoading && invitations?.length > 0 && (
            <div className="members-table-wrapper">
              <table className="members-table">
                <thead>
                  <tr>
                    <th>Invitee</th>
                    <th>Role</th>
                    <th>Invited By</th>
                    <th>Expires</th>
                    <th style={{ textAlign: 'right' }}>Cancel</th>
                  </tr>
                </thead>
                <tbody>
                  {invitations.map((inv) => (
                    <tr key={inv.id}>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <Mail size={16} color="var(--text-secondary)" />
                          <span>{inv.inviteeEmail}</span>
                        </div>
                      </td>
                      <td>
                        <RoleBadge role={inv.role} />
                      </td>
                      <td style={{ color: 'var(--text-secondary)' }}>
                        {inv.invitedByName || 'Admin'}
                      </td>
                      <td style={{ color: 'var(--text-secondary)' }}>
                        {formatDateTime(inv.expiresAt)}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <button
                          type="button"
                          className="icon-button"
                          style={{ color: '#ef4444' }}
                          onClick={() => setInvitationToCancel(inv)}
                          title="Cancel invitation"
                          aria-label={`Cancel invitation for ${inv.inviteeEmail}`}
                        >
                          <Trash2 size={16} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      {/* Confirm member removal */}
      <ConfirmDialog
        isOpen={Boolean(memberToRemove)}
        onClose={() => setMemberToRemove(null)}
        onConfirm={handleRemoveMemberConfirm}
        title="Remove Member"
        message={`Are you sure you want to remove ${memberToRemove?.name} from this workspace? They will lose access to all its projects and tasks.`}
        confirmText="Remove"
        isDestructive
        isLoading={removingMember}
      />

      {/* Confirm cancel invitation */}
      <ConfirmDialog
        isOpen={Boolean(invitationToCancel)}
        onClose={() => setInvitationToCancel(null)}
        onConfirm={handleCancelInvitationConfirm}
        title="Cancel Invitation"
        message={`Cancel the pending invitation for ${invitationToCancel?.inviteeEmail}?`}
        confirmText="Cancel Invitation"
        isDestructive
        isLoading={cancellingInvitation}
      />
    </div>
  );
}