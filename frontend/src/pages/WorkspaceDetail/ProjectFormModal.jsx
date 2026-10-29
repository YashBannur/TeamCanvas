
import { useState, useEffect } from 'react';
import { Modal } from '../../components/ui/Modal';
import { FormField } from '../../components/ui/FormField';
import { projectService } from '../../services/projectService';
import { useToast } from '../../context/ToastContext';
import { parseApiError } from '../../utils/apiError';

export default function ProjectFormModal({
  isOpen,
  onClose,
  workspaceId,
  project = null,
  onSaved
}) {
  const { success, error: showErrorToast } = useToast();
  const isEditing = Boolean(project);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [startDate, setStartDate] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (project) {
      setName(project.name || '');
      setDescription(project.description || '');
      setStartDate(project.startDate || '');
      setDueDate(project.dueDate || '');
    } else {
      setName('');
      setDescription('');
      setStartDate('');
      setDueDate('');
    }
    setFieldErrors({});
  }, [project, isOpen]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errors = {};

    if (!name.trim()) {
      errors.name = 'Project name is required';
    } else if (name.trim().length > 150) {
      errors.name = 'Project name must not exceed 150 characters';
    }

    if (description.length > 2000) {
      errors.description = 'Description must not exceed 2000 characters';
    }

    if (startDate && dueDate && dueDate < startDate) {
      errors.dueDate = 'Due date must not be before start date';
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setSubmitting(true);
    setFieldErrors({});

    try {
      if (isEditing) {
        const payload = {};
        if (name.trim() !== project.name) payload.name = name.trim();
        if (description !== (project.description || '')) payload.description = description;
        if (startDate && startDate !== project.startDate) payload.startDate = startDate;
        if (dueDate && dueDate !== project.dueDate) payload.dueDate = dueDate;

        const updated = await projectService.update(project.id, payload);
        success(`Project "${updated.name}" updated successfully`);
        onSaved(updated);
      } else {
        const payload = {
          name: name.trim(),
          description: description.trim() || undefined,
          startDate: startDate || undefined,
          dueDate: dueDate || undefined
        };
        const created = await projectService.create(workspaceId, payload);
        success(`Project "${created.name}" created successfully`);
        onSaved(created);
      }
      onClose();
    } catch (err) {
      const parsed = parseApiError(err);
      if (parsed.fieldErrors && Object.keys(parsed.fieldErrors).length > 0) {
        setFieldErrors(parsed.fieldErrors);
      } else {
        showErrorToast(parsed.message || `Failed to ${isEditing ? 'update' : 'create'} project`);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? 'Edit Project' : 'Create Project'}
      maxWidth={520}
    >
      <form onSubmit={handleSubmit}>
        <FormField
          id="project-name"
          label="Project Name"
          required
          error={fieldErrors.name}
        >
          {(props) => (
            <input
              {...props}
              type="text"
              placeholder="e.g. Website Redesign"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={submitting}
              maxLength={150}
            />
          )}
        </FormField>

        <FormField
          id="project-description"
          label="Description"
          hint="Summary of scope, milestones and deliverables (optional)"
          error={fieldErrors.description}
        >
          {(props) => (
            <textarea
              {...props}
              rows={3}
              placeholder="Provide context for this project..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={submitting}
              maxLength={2000}
            />
          )}
        </FormField>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
          <FormField
            id="project-start-date"
            label="Start Date"
            error={fieldErrors.startDate}
            hint={isEditing && project?.startDate ? 'Cannot be cleared once set' : undefined}
          >
            {(props) => (
              <input
                {...props}
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                disabled={submitting}
              />
            )}
          </FormField>

          <FormField
            id="project-due-date"
            label="Due Date"
            error={fieldErrors.dueDate}
            hint={isEditing && project?.dueDate ? 'Cannot be cleared once set' : undefined}
          >
            {(props) => (
              <input
                {...props}
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                disabled={submitting}
              />
            )}
          </FormField>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
          <button
            type="button"
            className="button-secondary"
            onClick={onClose}
            disabled={submitting}
          >
            Cancel
          </button>
          <button
            type="submit"
            className="button-primary"
            disabled={submitting}
          >
            {submitting ? 'Saving...' : isEditing ? 'Save Changes' : 'Create Project'}
          </button>
        </div>
      </form>
    </Modal>
  );
}