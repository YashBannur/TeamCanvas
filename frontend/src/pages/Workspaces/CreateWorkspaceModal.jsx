import { useState } from 'react';
import { Modal } from '../../components/ui/Modal';
import { FormField } from '../../components/ui/FormField';
import { workspaceService } from '../../services/workspaceService';
import { useToast } from '../../context/ToastContext';
import { parseApiError } from '../../utils/apiError';

export default function CreateWorkspaceModal({ isOpen, onClose, onCreated }) {
  const { success, error: showErrorToast } = useToast();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const resetForm = () => {
    setName('');
    setDescription('');
    setFieldErrors({});
    setSubmitting(false);
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const clientErrors = {};

    if (!name.trim()) {
      clientErrors.name = 'Workspace name is required';
    } else if (name.trim().length > 100) {
      clientErrors.name = 'Workspace name must not exceed 100 characters';
    }

    if (description.length > 500) {
      clientErrors.description = 'Description must not exceed 500 characters';
    }

    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }

    setSubmitting(true);
    setFieldErrors({});

    try {
      const created = await workspaceService.create({
        name: name.trim(),
        description: description.trim() || undefined
      });
      success(`Workspace "${created.name}" created successfully`);
      handleClose();
      onCreated(created);
    } catch (err) {
      const parsed = parseApiError(err);
      if (parsed.fieldErrors && Object.keys(parsed.fieldErrors).length > 0) {
        setFieldErrors(parsed.fieldErrors);
      } else {
        showErrorToast(parsed.message || 'Failed to create workspace');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Create Workspace" maxWidth={500}>
      <form onSubmit={handleSubmit}>
        <FormField
          id="ws-name"
          label="Workspace Name"
          required
          error={fieldErrors.name}
        >
          {(props) => (
            <input
              {...props}
              type="text"
              placeholder="e.g. Acme Engineering"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={submitting}
              maxLength={100}
            />
          )}
        </FormField>

        <FormField
          id="ws-description"
          label="Description"
          hint="Brief overview of this workspace's purpose (optional)"
          error={fieldErrors.description}
        >
          {(props) => (
            <textarea
              {...props}
              rows={3}
              placeholder="What does your team work on here?"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={submitting}
              maxLength={500}
            />
          )}
        </FormField>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
          <button
            type="button"
            className="button-secondary"
            onClick={handleClose}
            disabled={submitting}
          >
            Cancel
          </button>
          <button
            type="submit"
            className="button-primary"
            disabled={submitting}
          >
            {submitting ? 'Creating...' : 'Create Workspace'}
          </button>
        </div>
      </form>
    </Modal>
  );
}