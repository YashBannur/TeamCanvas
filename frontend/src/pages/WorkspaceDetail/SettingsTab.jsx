import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { workspaceService } from '../../services/workspaceService';
import { useToast } from '../../context/ToastContext';
import { FormField } from '../../components/ui/FormField';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { WORKSPACE_ROLES } from '../../constants/roles';
import {
  canManageWorkspace,
  canDeleteWorkspace,
  canLeaveWorkspace
} from '../../utils/permissions';
import { parseApiError } from '../../utils/apiError';

export default function SettingsTab({ workspace, onWorkspaceUpdated }) {
  const navigate = useNavigate();
  const { success, error: showErrorToast } = useToast();

  const isOwner = workspace.myRole === WORKSPACE_ROLES.OWNER;
  const canEdit = canManageWorkspace(workspace.myRole);
  const canLeave = canLeaveWorkspace(workspace.myRole);
  const canDelete = canDeleteWorkspace(workspace.myRole);

  const [name, setName] = useState(workspace.name || '');
  const [description, setDescription] = useState(workspace.description || '');
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);

  // Leave confirm modal
  const [isLeaveOpen, setIsLeaveOpen] = useState(false);
  const [leaving, setLeaving] = useState(false);

  // Delete confirm modal
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const handleUpdate = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setFieldErrors({ name: 'Workspace name is required' });
      return;
    }

    setSaving(true);
    setFieldErrors({});

    try {
      const updated = await workspaceService.update(workspace.id, {
        name: name.trim(),
        description: description
      });
      success('Workspace settings updated successfully');
      onWorkspaceUpdated(updated);
    } catch (err) {
      const parsed = parseApiError(err);
      if (parsed.fieldErrors && Object.keys(parsed.fieldErrors).length > 0) {
        setFieldErrors(parsed.fieldErrors);
      } else {
        showErrorToast(parsed.message || 'Failed to update workspace');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleLeaveConfirm = async () => {
    setLeaving(true);
    try {
      await workspaceService.leave(workspace.id);
      success('You have left the workspace');
      navigate('/workspaces', { replace: true });
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to leave workspace');
    } finally {
      setLeaving(false);
      setIsLeaveOpen(false);
    }
  };

  const handleDeleteConfirm = async () => {
    setDeleting(true);
    try {
      await workspaceService.delete(workspace.id);
      success('Workspace deleted successfully');
      navigate('/workspaces', { replace: true });
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to delete workspace');
    } finally {
      setDeleting(false);
      setIsDeleteOpen(false);
    }
  };

  return (
    <div className="settings-container">
      {/* General Settings */}
      {canEdit ? (
        <form onSubmit={handleUpdate} className="settings-card">
          <h3 className="settings-card__title">General Settings</h3>
          <p className="settings-card__desc">
            Update your workspace name and public team description.
          </p>

          <FormField
            id="ws-settings-name"
            label="Workspace Name"
            required
            error={fieldErrors.name}
          >
            {(props) => (
              <input
                {...props}
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                disabled={saving}
                maxLength={100}
              />
            )}
          </FormField>

          <FormField
            id="ws-settings-desc"
            label="Description"
            hint="Set blank to clear the description."
            error={fieldErrors.description}
          >
            {(props) => (
              <textarea
                {...props}
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                disabled={saving}
                maxLength={500}
              />
            )}
          </FormField>

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <button
              type="submit"
              className="button-primary"
              disabled={saving}
            >
              {saving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      ) : (
        <div className="settings-card">
          <h3 className="settings-card__title">General Information</h3>
          <p className="settings-card__desc">
            Only workspace owners and administrators can change these settings.
          </p>
          <div>
            <strong>Name:</strong> {workspace.name}
          </div>
          <div>
            <strong>Description:</strong> {workspace.description || 'None'}
          </div>
        </div>
      )}

      {/* Leave Workspace Action (Admin / Member) */}
      {canLeave && (
        <div className="settings-card settings-card--danger">
          <h3 className="settings-card__title" style={{ color: '#ef4444' }}>
            Leave Workspace
          </h3>
          <p className="settings-card__desc">
            You will lose access to all projects, Kanban boards, and tasks in this workspace.
          </p>
          <div>
            <button
              type="button"
              className="button-secondary"
              style={{ borderColor: '#ef4444', color: '#ef4444' }}
              onClick={() => setIsLeaveOpen(true)}
            >
              Leave Workspace
            </button>
          </div>
        </div>
      )}

      {/* Delete Workspace Action (Owner Only) */}
      {canDelete && (
        <div className="settings-card settings-card--danger">
          <h3 className="settings-card__title" style={{ color: '#ef4444' }}>
            Delete Workspace
          </h3>
          <p className="settings-card__desc">
            Permanently delete this workspace, including all associated projects, tasks, and member lists. This action cannot be undone.
          </p>
          <div>
            <button
              type="button"
              className="button-primary"
              style={{ backgroundColor: '#ef4444' }}
              onClick={() => setIsDeleteOpen(true)}
            >
              Delete Workspace
            </button>
          </div>
        </div>
      )}

      {/* Leave Confirm Dialog */}
      <ConfirmDialog
        isOpen={isLeaveOpen}
        onClose={() => setIsLeaveOpen(false)}
        onConfirm={handleLeaveConfirm}
        title="Leave Workspace"
        message={`Are you sure you want to leave "${workspace.name}"? You will need an invite to rejoin.`}
        confirmText="Leave"
        isDestructive
        isLoading={leaving}
      />

      {/* Delete Confirm Dialog */}
      <ConfirmDialog
        isOpen={isDeleteOpen}
        onClose={() => setIsDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        title="Delete Workspace"
        message={`Are you absolutely sure you want to delete "${workspace.name}"? All projects, tasks, and board positions will be permanently wiped.`}
        confirmText="Delete Workspace"
        isDestructive
        isLoading={deleting}
      />
    </div>
  );
}