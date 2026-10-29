import { useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { FolderKanban, Users, Settings as SettingsIcon } from 'lucide-react';
import { workspaceService } from '../../services/workspaceService';
import { useFetch } from '../../hooks/useFetch';
import { RoleBadge } from '../../components/ui/Badge';
import { Skeleton } from '../../components/ui/Skeleton';
import { ErrorState } from '../../components/ui/ErrorState';
import ProjectsTab from './ProjectsTab';
import MembersTab from './MembersTab';
import SettingsTab from './SettingsTab';
import './WorkspaceDetail.css';

export default function WorkspaceDetail() {
  const { workspaceId } = useParams();
  const [activeTab, setActiveTab] = useState('projects');

  const fetchWorkspace = useCallback(
    () => workspaceService.getById(Number(workspaceId)),
    [workspaceId]
  );

  const {
    data: workspace,
    loading,
    error,
    setData: setWorkspace
  } = useFetch(fetchWorkspace, [workspaceId]);

  if (loading) {
    return (
      <div className="workspace-detail">
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <Skeleton width="300px" height="32px" />
          <Skeleton width="500px" height="18px" />
        </div>
        <div style={{ display: 'flex', gap: '12px', marginTop: '16px' }}>
          <Skeleton width="100px" height="36px" />
          <Skeleton width="100px" height="36px" />
          <Skeleton width="100px" height="36px" />
        </div>
      </div>
    );
  }

  // Contract: 404 means either not found or caller is not a member -> neutral message
  if (error) {
    return (
      <div className="workspace-detail">
        <ErrorState
          title="Workspace Not Found"
          message={
            error.status === 404
              ? 'This workspace does not exist or you do not have permission to view it.'
              : error.message
          }
          action={
            <Link to="/workspaces" className="button-secondary">
              Back to Workspaces
            </Link>
          }
        />
      </div>
    );
  }

  if (!workspace) return null;

  return (
    <div className="workspace-detail">
      <header className="workspace-detail__header">
        <div className="workspace-detail__info">
          <div className="workspace-detail__title-row">
            <h1 className="workspace-detail__title">{workspace.name}</h1>
            <RoleBadge role={workspace.myRole} />
          </div>
          {workspace.description && (
            <p className="workspace-detail__description">{workspace.description}</p>
          )}
        </div>
      </header>

      <nav className="workspace-tabs" aria-label="Workspace tabs">
        <button
          type="button"
          className={`workspace-tab-btn ${activeTab === 'projects' ? 'active' : ''}`}
          onClick={() => setActiveTab('projects')}
        >
          <FolderKanban size={18} />
          Projects
        </button>
        <button
          type="button"
          className={`workspace-tab-btn ${activeTab === 'members' ? 'active' : ''}`}
          onClick={() => setActiveTab('members')}
        >
          <Users size={18} />
          Members
        </button>
        <button
          type="button"
          className={`workspace-tab-btn ${activeTab === 'settings' ? 'active' : ''}`}
          onClick={() => setActiveTab('settings')}
        >
          <SettingsIcon size={18} />
          Settings
        </button>
      </nav>

      {activeTab === 'projects' && <ProjectsTab workspace={workspace} />}

      {activeTab === 'members' && <MembersTab workspace={workspace} />}

      {activeTab === 'settings' && (
        <SettingsTab
          workspace={workspace}
          onWorkspaceUpdated={(updated) => setWorkspace(updated)}
        />
      )}
    </div>
  );
}