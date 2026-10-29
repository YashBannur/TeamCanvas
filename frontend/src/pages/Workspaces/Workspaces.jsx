import { useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Users, Briefcase } from 'lucide-react';
import { workspaceService } from '../../services/workspaceService';
import { useFetch } from '../../hooks/useFetch';
import { RoleBadge } from '../../components/ui/Badge';
import { CardSkeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import CreateWorkspaceModal from './CreateWorkspaceModal';
import './Workspaces.css';

export default function Workspaces() {
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const fetchWorkspaces = useCallback(() => workspaceService.getAll(), []);
  const { data: workspaces, loading, error, reload } = useFetch(fetchWorkspaces);

  return (
    <div className="workspaces-page">
      <header className="workspaces-header">
        <div>
          <h1 className="workspaces-title">Workspaces</h1>
          <p className="workspaces-subtitle">
            Collaborate on projects, tasks, and boards with your team members.
          </p>
        </div>
        <button
          type="button"
          className="button-primary"
          onClick={() => setIsCreateOpen(true)}
        >
          <Plus size={18} />
          Create Workspace
        </button>
      </header>

      {loading && (
        <div className="workspaces-grid">
          <CardSkeleton />
          <CardSkeleton />
          <CardSkeleton />
        </div>
      )}

      {error && !loading && (
        <ErrorState
          title="Failed to load workspaces"
          message={error.message || 'Please check your connection and try again.'}
          onRetry={reload}
        />
      )}

      {!loading && !error && workspaces?.length === 0 && (
        <EmptyState
          icon={Briefcase}
          title="No workspaces yet"
          description="Get started by creating your first workspace, or accept an invitation from your team."
          action={
            <button
              type="button"
              className="button-primary"
              onClick={() => setIsCreateOpen(true)}
            >
              <Plus size={18} />
              Create your first workspace
            </button>
          }
        />
      )}

      {!loading && !error && workspaces?.length > 0 && (
        <div className="workspaces-grid">
          {workspaces.map((ws) => (
            <Link
              key={ws.id}
              to={`/workspaces/${ws.id}`}
              className="workspace-card"
            >
              <div className="workspace-card__header">
                <h2 className="workspace-card__title" title={ws.name}>
                  {ws.name}
                </h2>
                <RoleBadge role={ws.myRole} />
              </div>

              <p className="workspace-card__description">
                {ws.description || 'No description provided.'}
              </p>

              <div className="workspace-card__footer">
                <span className="workspace-card__meta-item">
                  <Users size={16} />
                  {ws.memberCount} {ws.memberCount === 1 ? 'member' : 'members'}
                </span>
                <span>Open &rarr;</span>
              </div>
            </Link>
          ))}
        </div>
      )}

      <CreateWorkspaceModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onCreated={() => reload()}
      />
    </div>
  );
}