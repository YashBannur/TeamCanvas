import { useState, useEffect } from 'react';
import { Trash2 } from 'lucide-react';
import { Modal } from '../../components/ui/Modal';
import { FormField } from '../../components/ui/FormField';
import { PriorityBadge, StatusBadge } from '../../components/ui/Badge';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { TASK_PRIORITIES } from '../../constants/taskPriority';
import { taskService } from '../../services/taskService';
import { useToast } from '../../context/ToastContext';
import { useAuth } from '../../context/AuthContext';
import { formatDateTime } from '../../utils/date';
import { parseApiError } from '../../utils/apiError';
import {
  canAssignTask,
  canDeleteTask,
  canEditTask
} from '../../utils/permissions';
import TaskComments from './TaskComments';
import TaskActivity from './TaskActivity';
import './TaskComments.css';

export default function TaskModal({
  isOpen,
  onClose,
  taskId,
  navigation = null,
  workspaceMembers = [],
  workspaceRole,
  canEdit,
  canAssign,
  canDelete,
  isArchived,
  onTaskUpdated,
  onTaskDeleted
}) {
  const { user } = useAuth();
  const { success, error: showErrorToast } = useToast();

  const [task, setTask] = useState(null);
  const [loadingTask, setLoadingTask] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [activeTab, setActiveTab] = useState('details');

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState(TASK_PRIORITIES.MEDIUM);
  const [assigneeId, setAssigneeId] = useState('');
  const [startDate, setStartDate] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const loadTask = async (id) => {
    setLoadingTask(true);
    try {
      const data = await taskService.getById(id);
      setTask(data);
      setTitle(data.title || '');
      setDescription(data.description || '');
      setPriority(data.priority || TASK_PRIORITIES.MEDIUM);
      setAssigneeId(data.assigneeId ? String(data.assigneeId) : '');
      setStartDate(data.startDate || '');
      setDueDate(data.dueDate || '');
      setFieldErrors({});
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to load task details');
      onClose();
    } finally {
      setLoadingTask(false);
    }
  };

  useEffect(() => {
    if (isOpen && taskId) {
      setActiveTab(navigation?.tab || 'details');
      loadTask(taskId);
    } else {
      setTask(null);
      setActiveTab('details');
    }
    // loadTask is intentionally omitted; the effect should run for task/navigation changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    isOpen,
    taskId,
    navigation?.tab,
    navigation?.eventId,
    navigation?.commentId
  ]);

  const resolvedCanEdit = task
    ? canEditTask({
        myRole: workspaceRole,
        task,
        currentUserId: user?.id,
        isProjectArchived: isArchived
      })
    : canEdit;

  const resolvedCanAssign = task
    ? canAssignTask({
        myRole: workspaceRole,
        task,
        currentUserId: user?.id,
        isProjectArchived: isArchived
      })
    : canAssign;

  const resolvedCanDelete = task
    ? canDeleteTask({
        myRole: workspaceRole,
        task,
        currentUserId: user?.id,
        isProjectArchived: isArchived
      })
    : canDelete;

  const handleSave = async (event) => {
    event.preventDefault();

    if (!task || !resolvedCanEdit) return;

    if (!title.trim()) {
      setFieldErrors({ title: 'Task title is required' });
      return;
    }

    setSubmitting(true);
    setFieldErrors({});

    const payload = {
      version: task.version
    };

    if (title.trim() !== task.title) payload.title = title.trim();
    if (description !== (task.description || '')) payload.description = description;
    if (priority !== task.priority) payload.priority = priority;
    if (startDate && startDate !== task.startDate) payload.startDate = startDate;
    if (dueDate && dueDate !== task.dueDate) payload.dueDate = dueDate;

    if (resolvedCanAssign) {
      const currentAssignee = task.assigneeId ? String(task.assigneeId) : '';
      if (assigneeId !== currentAssignee) {
        if (assigneeId === '') {
          payload.unassign = true;
        } else {
          payload.assigneeId = Number(assigneeId);
        }
      }
    }

    if (Object.keys(payload).length === 1) {
      onClose();
      return;
    }

    try {
      const updated = await taskService.update(task.id, payload);
      success('Task updated');
      onTaskUpdated(updated);
      onClose();
    } catch (err) {
      const parsed = parseApiError(err);
      if (parsed.status === 409) {
        showErrorToast('Task was modified by another user. Reloading latest...');
        loadTask(task.id);
      } else if (parsed.fieldErrors && Object.keys(parsed.fieldErrors).length > 0) {
        setFieldErrors(parsed.fieldErrors);
      } else {
        showErrorToast(parsed.message || 'Failed to update task');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!task || !resolvedCanDelete) return;

    setDeleting(true);
    try {
      await taskService.delete(task.id);
      success('Task deleted');
      setIsDeleteOpen(false);
      onClose();
      onTaskDeleted(task.id);
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to delete task');
    } finally {
      setDeleting(false);
    }
  };

  if (!isOpen) return null;

  const isReadOnly = !resolvedCanEdit || isArchived;
  const isAssigneeLocked = isReadOnly || !resolvedCanAssign;

  return (
    <>
      <Modal isOpen={isOpen} onClose={onClose} title="Task Details" maxWidth={560}>
        {loadingTask || !task ? (
          <p style={{ color: 'var(--text-secondary)' }}>Loading task details...</p>
        ) : (
          <>
            <div className="task-modal-tabs" role="tablist" aria-label="Task sections">
              <button
                type="button"
                role="tab"
                aria-selected={activeTab === 'details'}
                className="task-modal-tab"
                onClick={() => setActiveTab('details')}
              >
                Details
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={activeTab === 'comments'}
                className="task-modal-tab"
                onClick={() => setActiveTab('comments')}
              >
                Comments
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={activeTab === 'activity'}
                className="task-modal-tab"
                onClick={() => setActiveTab('activity')}
              >
                Activity
              </button>
            </div>

            {activeTab === 'details' ? (
              <form onSubmit={handleSave}>
                <div
                  style={{
                    display: 'flex',
                    gap: '8px',
                    marginBottom: '16px',
                    alignItems: 'center'
                  }}
                >
                  <StatusBadge status={task.status} />
                  <PriorityBadge priority={task.priority} />
                </div>

                <FormField id="task-title" label="Title" required error={fieldErrors.title}>
                  {(props) => (
                    <input
                      {...props}
                      type="text"
                      value={title}
                      onChange={(event) => setTitle(event.target.value)}
                      disabled={isReadOnly || submitting}
                      maxLength={200}
                    />
                  )}
                </FormField>

                <FormField
                  id="task-description"
                  label="Description"
                  error={fieldErrors.description}
                >
                  {(props) => (
                    <textarea
                      {...props}
                      rows={4}
                      value={description}
                      onChange={(event) => setDescription(event.target.value)}
                      disabled={isReadOnly || submitting}
                      maxLength={5000}
                      placeholder="Add details for this task..."
                    />
                  )}
                </FormField>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                  <FormField id="task-priority" label="Priority">
                    {(props) => (
                      <select
                        {...props}
                        value={priority}
                        onChange={(event) => setPriority(event.target.value)}
                        disabled={isReadOnly || submitting}
                      >
                        <option value={TASK_PRIORITIES.LOW}>Low</option>
                        <option value={TASK_PRIORITIES.MEDIUM}>Medium</option>
                        <option value={TASK_PRIORITIES.HIGH}>High</option>
                        <option value={TASK_PRIORITIES.URGENT}>Urgent</option>
                      </select>
                    )}
                  </FormField>

                  <FormField id="task-assignee" label="Assignee">
                    {(props) => (
                      <select
                        {...props}
                        value={assigneeId}
                        onChange={(event) => setAssigneeId(event.target.value)}
                        disabled={isAssigneeLocked || submitting}
                      >
                        <option value="">Unassigned</option>
                        {workspaceMembers.map((member) => (
                          <option key={member.userId} value={String(member.userId)}>
                            {member.name}
                          </option>
                        ))}
                      </select>
                    )}
                  </FormField>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                  <FormField
                    id="task-start"
                    label="Start Date"
                    hint={task.startDate ? 'Cannot be cleared once set' : undefined}
                  >
                    {(props) => (
                      <input
                        {...props}
                        type="date"
                        value={startDate}
                        onChange={(event) => setStartDate(event.target.value)}
                        disabled={isReadOnly || submitting}
                      />
                    )}
                  </FormField>

                  <FormField
                    id="task-due"
                    label="Due Date"
                    hint={task.dueDate ? 'Cannot be cleared once set' : undefined}
                  >
                    {(props) => (
                      <input
                        {...props}
                        type="date"
                        value={dueDate}
                        onChange={(event) => setDueDate(event.target.value)}
                        disabled={isReadOnly || submitting}
                      />
                    )}
                  </FormField>
                </div>

                <div
                  style={{
                    fontSize: '0.8rem',
                    color: 'var(--text-secondary)',
                    margin: '16px 0',
                    paddingTop: '12px',
                    borderTop: '1px solid var(--border-color)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '4px'
                  }}
                >
                  <span>Reporter: <strong>{task.reporterName}</strong></span>
                  <span>Created: {formatDateTime(task.createdAt)}</span>
                  <span>Last updated: {formatDateTime(task.updatedAt)}</span>
                </div>

                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginTop: '20px'
                  }}
                >
                  {resolvedCanDelete && !isArchived ? (
                    <button
                      type="button"
                      className="icon-button"
                      style={{ color: '#ef4444' }}
                      onClick={() => setIsDeleteOpen(true)}
                      title="Delete task"
                      aria-label="Delete task"
                    >
                      <Trash2 size={18} />
                    </button>
                  ) : (
                    <div />
                  )}

                  <div style={{ display: 'flex', gap: '10px' }}>
                    <button
                      type="button"
                      className="button-secondary"
                      onClick={onClose}
                      disabled={submitting}
                    >
                      {isReadOnly && !resolvedCanAssign ? 'Close' : 'Cancel'}
                    </button>

                    {(!isReadOnly || resolvedCanAssign) && (
                      <button
                        type="submit"
                        className="button-primary"
                        disabled={submitting}
                      >
                        {submitting ? 'Saving...' : 'Save Changes'}
                      </button>
                    )}
                  </div>
                </div>
              </form>
            ) : activeTab === 'comments' ? (
              <TaskComments
                taskId={task.id}
                workspaceMembers={workspaceMembers}
                currentUserId={user?.id}
                workspaceRole={workspaceRole}
                isArchived={isArchived}
                focusCommentId={navigation?.commentId}
              />
            ) : (
              <TaskActivity
                taskId={task.id}
                workspaceMembers={workspaceMembers}
                focusEventId={navigation?.eventId}
              />
            )}
          </>
        )}
      </Modal>

      <ConfirmDialog
        isOpen={isDeleteOpen}
        onClose={() => setIsDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        title="Delete Task"
        message={`Delete "${task?.title}" permanently?`}
        confirmText="Delete"
        isDestructive
        isLoading={deleting}
      />
    </>
  );
}