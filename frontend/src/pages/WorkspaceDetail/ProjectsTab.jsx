import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Calendar, Edit2, Archive, RotateCcw, Trash2, FolderKanban } from 'lucide-react';
import { projectService } from '../../services/projectService';
import { useToast } from '../../context/ToastContext';
import { useFetch } from '../../hooks/useFetch';
import { PROJECT_STATUS } from '../../constants/projectStatus';
import { canManageProjects } from '../../utils/permissions';
import { formatDate } from '../../utils/date';
import { StatusBadge } from '../../components/ui/Badge';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { CardSkeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import ProjectFormModal from './ProjectFormModal';
import { parseApiError } from '../../utils/apiError';
import './ProjectsTab.css';

export default function ProjectsTab({ workspace }) {
  const navigate = useNavigate();
  const { success, error: showErrorToast } = useToast();
  const [selectedStatus, setSelectedStatus] = useState(PROJECT_STATUS.ACTIVE);

  const canManage = canManageProjects(workspace.myRole);

  const fetchProjects = useCallback(
    () => projectService.getByWorkspace(workspace.id, selectedStatus),
    [workspace.id, selectedStatus]
  );
  const { data: projects, loading, error, reload } = useFetch(fetchProjects, [selectedStatus]);

  // Modal states
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingProject, setEditingProject] = useState(null);

  // Archive / Restore confirm dialog
  const [projectToToggleStatus, setProjectToToggleStatus] = useState(null);
  const [togglingStatus, setTogglingStatus] = useState(false);

  // Delete confirm dialog
  const [projectToDelete, setProjectToDelete] = useState(null);
  const [deletingProject, setDeletingProject] = useState(false);

  const handleOpenCreate = () => {
    setEditingProject(null);
    setIsFormOpen(true);
  };

  const handleOpenEdit = (e, proj) => {
    e.preventDefault();
    e.stopPropagation();
    setEditingProject(proj);
    setIsFormOpen(true);
  };

  const handleToggleStatusClick = (e, proj) => {
    e.preventDefault();
    e.stopPropagation();
    setProjectToToggleStatus(proj);
  };

  const handleToggleStatusConfirm = async () => {
    if (!projectToToggleStatus) return;
    setTogglingStatus(true);
    const newStatus =
      projectToToggleStatus.status === PROJECT_STATUS.ACTIVE
        ? PROJECT_STATUS.ARCHIVED
        : PROJECT_STATUS.ACTIVE;

    try {
      await projectService.update(projectToToggleStatus.id, { status: newStatus });
      success(
        `Project "${projectToToggleStatus.name}" ${
          newStatus === PROJECT_STATUS.ARCHIVED ? 'archived' : 'restored'
        }`
      );
      setProjectToToggleStatus(null);
      reload();
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to update project status');
    } finally {
      setTogglingStatus(false);
    }
  };

  const handleDeleteClick = (e, proj) => {
    e.preventDefault();
    e.stopPropagation();
    setProjectToDelete(proj);
  };

  const handleDeleteConfirm = async () => {
    if (!projectToDelete) return;
    setDeletingProject(true);
    try {
      await projectService.delete(projectToDelete.id);
      success(`Project "${projectToDelete.name}" deleted`);
      setProjectToDelete(null);
      reload();
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to delete project');
    } finally {
      setDeletingProject(false);
    }
  };

  return (
    <div className="projects-tab">
      <div className="projects-tab__toolbar">
        <div className="projects-tab__filter-group" role="tablist" aria-label="Project status filter">
          <button
            type="button"
            className={`projects-tab__filter-btn ${selectedStatus === PROJECT_STATUS.ACTIVE ? 'active' : ''}`}
            onClick={() => setSelectedStatus(PROJECT_STATUS.ACTIVE)}
          >
            Active
          </button>
          <button
            type="button"
            className={`projects-tab__filter-btn ${selectedStatus === PROJECT_STATUS.ARCHIVED ? 'active' : ''}`}
            onClick={() => setSelectedStatus(PROJECT_STATUS.ARCHIVED)}
          >
            Archived
          </button>
        </div>

        {canManage && (
          <button
            type="button"
            className="button-primary"
            onClick={handleOpenCreate}
          >
            <Plus size={18} />
            Create Project
          </button>
        )}
      </div>

      {loading && (
        <div className="projects-grid">
          <CardSkeleton />
          <CardSkeleton />
          <CardSkeleton />
        </div>
      )}

      {error && !loading && (
        <ErrorState
          title="Failed to load projects"
          message={error.message}
          onRetry={reload}
        />
      )}

      {!loading && !error && projects?.length === 0 && (
        <EmptyState
          icon={FolderKanban}
          title={
            selectedStatus === PROJECT_STATUS.ACTIVE
              ? 'No active projects'
              : 'No archived projects'
          }
          description={
            selectedStatus === PROJECT_STATUS.ACTIVE
              ? 'Get started by creating a project to organize tasks and track team progress.'
              : 'Projects you archive will appear here.'
          }
          action={
            canManage && selectedStatus === PROJECT_STATUS.ACTIVE ? (
              <button
                type="button"
                className="button-primary"
                onClick={handleOpenCreate}
              >
                <Plus size={18} />
                Create your first project
              </button>
            ) : null
          }
        />
      )}

      {!loading && !error && projects?.length > 0 && (
        <div className="projects-grid">
          {projects.map((proj) => (
            <div
              key={proj.id}
              className="project-card"
              role="button"
              tabIndex={0}
              onClick={() => navigate(`/projects/${proj.id}`)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') navigate(`/projects/${proj.id}`);
              }}
            >
              <div className="project-card__header">
                <div className="project-card__title-area">
                  <h3 className="project-card__title" title={proj.name}>
                    {proj.name}
                  </h3>
                  <span className="project-card__creator">
                    Created by {proj.createdByName || 'Team Member'}
                  </span>
                </div>

                <div className="project-card__actions" onClick={(e) => e.stopPropagation()}>
                  <StatusBadge status={proj.status} />

                  {canManage && (
                    <>
                      <button
                        type="button"
                        className="icon-button"
                        onClick={(e) => handleOpenEdit(e, proj)}
                        title="Edit project"
                        aria-label={`Edit ${proj.name}`}
                      >
                        <Edit2 size={15} />
                      </button>

                      <button
                        type="button"
                        className="icon-button"
                        onClick={(e) => handleToggleStatusClick(e, proj)}
                        title={proj.status === PROJECT_STATUS.ACTIVE ? 'Archive project' : 'Restore project'}
                        aria-label={proj.status === PROJECT_STATUS.ACTIVE ? `Archive ${proj.name}` : `Restore ${proj.name}`}
                      >
                        {proj.status === PROJECT_STATUS.ACTIVE ? (
                          <Archive size={15} />
                        ) : (
                          <RotateCcw size={15} />
                        )}
                      </button>

                      <button
                        type="button"
                        className="icon-button"
                        style={{ color: '#ef4444' }}
                        onClick={(e) => handleDeleteClick(e, proj)}
                        title="Delete project"
                        aria-label={`Delete ${proj.name}`}
                      >
                        <Trash2 size={15} />
                      </button>
                    </>
                  )}
                </div>
              </div>

              <p className="project-card__description">
                {proj.description || 'No description provided.'}
              </p>

              <div className="project-card__progress-block">
                <div className="project-card__progress-meta">
                  <span>Progress</span>
                  <span>
                    {proj.completedTasks ?? 0} of {proj.totalTasks ?? 0} tasks done
                  </span>
                </div>
                <ProgressBar value={proj.progressPercent ?? 0} />
              </div>

              <div className="project-card__footer">
                <div className="project-card__date">
                  <Calendar size={15} />
                  <span>
                    {proj.dueDate ? `Due ${formatDate(proj.dueDate)}` : 'No due date'}
                  </span>
                </div>
                <span style={{ fontWeight: 500 }}>View board &rarr;</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create / Edit Project Modal */}
      <ProjectFormModal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        workspaceId={workspace.id}
        project={editingProject}
        onSaved={() => reload()}
      />

      {/* Archive / Restore Confirm Dialog */}
      <ConfirmDialog
        isOpen={Boolean(projectToToggleStatus)}
        onClose={() => setProjectToToggleStatus(null)}
        onConfirm={handleToggleStatusConfirm}
        title={
          projectToToggleStatus?.status === PROJECT_STATUS.ACTIVE
            ? 'Archive Project'
            : 'Restore Project'
        }
        message={
          projectToToggleStatus?.status === PROJECT_STATUS.ACTIVE
            ? `Are you sure you want to archive "${projectToToggleStatus?.name}"? Its board will become read-only until restored.`
            : `Restore "${projectToToggleStatus?.name}" to make its tasks editable again?`
        }
        confirmText={
          projectToToggleStatus?.status === PROJECT_STATUS.ACTIVE
            ? 'Archive'
            : 'Restore'
        }
        isLoading={togglingStatus}
      />

      {/* Delete Project Confirm Dialog */}
      <ConfirmDialog
        isOpen={Boolean(projectToDelete)}
        onClose={() => setProjectToDelete(null)}
        onConfirm={handleDeleteConfirm}
        title="Delete Project"
        message={`Are you sure you want to permanently delete "${projectToDelete?.name}"? All associated tasks and column orders will be deleted.`}
        confirmText="Delete Project"
        isDestructive
        isLoading={deletingProject}
      />
    </div>
  );
}