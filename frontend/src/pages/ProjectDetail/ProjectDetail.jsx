import { useState, useCallback, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  Kanban,
  List,
  Calendar,
  AlertTriangle,
  BarChart3,
  Activity
} from 'lucide-react';
import { projectService } from '../../services/projectService';
import { workspaceService } from '../../services/workspaceService';
import { taskService } from '../../services/taskService';
import { useAuth } from '../../context/AuthContext';
import { useFetch } from '../../hooks/useFetch';
import { useDebounce } from '../../hooks/useDebounce';
import { StatusBadge } from '../../components/ui/Badge';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { ErrorState } from '../../components/ui/ErrorState';
import { Skeleton } from '../../components/ui/Skeleton';
import {
  canEditTask,
  canAssignTask,
  canMoveTask,
  canDeleteTask
} from '../../utils/permissions';
import { formatDate } from '../../utils/date';
import { KanbanBoard } from './KanbanBoard';
import { TaskListView } from './TaskListView';
import ProjectAnalytics from './ProjectAnalytics';
import TaskModal from './TaskModal';
import ProjectActivity from './ProjectActivity';
import './ProjectDetail.css';

export default function ProjectDetail() {
  const { projectId } = useParams();
  const { user } = useAuth();

  const [viewMode, setViewMode] = useState('board');

  // Filter state
  const [searchTerm, setSearchTerm] = useState('');
  const debouncedSearch = useDebounce(searchTerm, 300);
  const [assigneeFilter, setAssigneeFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [dueFrom, setDueFrom] = useState('');
  const [dueTo, setDueTo] = useState('');

  // List view sorting and paging
  const [page, setPage] = useState(0);
  const [sortBy, setSortBy] = useState('createdAt');
  const [direction, setDirection] = useState('desc');

  // Selected task and activity navigation target
  const [selectedTaskId, setSelectedTaskId] = useState(null);
  const [activityTarget, setActivityTarget] = useState(null);

  const fetchProject = useCallback(
    () => projectService.getById(Number(projectId)),
    [projectId]
  );

  const {
    data: project,
    loading: projectLoading,
    error: projectError,
    reload: reloadProject
  } = useFetch(fetchProject, [projectId]);

  const fetchWorkspaceAndMembers = useCallback(async () => {
    if (!project?.workspaceId) return null;

    const [workspaceData, membersData] = await Promise.all([
      workspaceService.getById(project.workspaceId),
      workspaceService.getMembers(project.workspaceId)
    ]);

    return {
      workspace: workspaceData,
      members: membersData
    };
  }, [project?.workspaceId]);

  const { data: wsData } = useFetch(fetchWorkspaceAndMembers, [project?.workspaceId]);

  const workspace = wsData?.workspace;
  const members = wsData?.members || [];
  const isArchived = project?.status === 'ARCHIVED';

  const buildFilterParams = useCallback(() => {
    const params = {};

    if (debouncedSearch.trim()) params.q = debouncedSearch.trim();
    if (priorityFilter) params.priority = priorityFilter;
    if (dueFrom) params.dueFrom = dueFrom;
    if (dueTo) params.dueTo = dueTo;

    if (assigneeFilter === 'unassigned') {
      params.unassigned = true;
    } else if (assigneeFilter) {
      params.assigneeId = Number(assigneeFilter);
    }

    return params;
  }, [debouncedSearch, priorityFilter, dueFrom, dueTo, assigneeFilter]);

  const fetchBoard = useCallback(() => {
    if (!projectId || viewMode !== 'board') return Promise.resolve(null);
    return taskService.getBoard(projectId, buildFilterParams());
  }, [projectId, viewMode, buildFilterParams]);

  const {
    data: boardData,
    loading: boardLoading,
    error: boardError,
    reload: reloadBoard
  } = useFetch(fetchBoard, [fetchBoard]);

  const fetchList = useCallback(() => {
    if (!projectId || viewMode !== 'list') return Promise.resolve(null);

    const params = {
      ...buildFilterParams(),
      page,
      size: 20,
      sortBy,
      direction
    };

    return taskService.getByProjectPaged(projectId, params);
  }, [projectId, viewMode, buildFilterParams, page, sortBy, direction]);

  const {
    data: listData,
    loading: listLoading,
    reload: reloadList
  } = useFetch(fetchList, [fetchList]);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, assigneeFilter, priorityFilter, dueFrom, dueTo]);

  const refreshAll = () => {
    reloadProject();

    if (viewMode === 'board') {
      reloadBoard();
    } else if (viewMode === 'list') {
      reloadList();
    }
  };

  const userCanMove = (task) => {
    if (!workspace) return false;

    return canMoveTask({
      myRole: workspace.myRole,
      task,
      currentUserId: user?.id,
      isProjectArchived: isArchived
    });
  };

  const openTaskFromBoardOrList = (task) => {
    setActivityTarget(null);
    setSelectedTaskId(task.id);
  };

  const handleOpenActivity = (target) => {
    setActivityTarget(target);
    setSelectedTaskId(target.taskId);
  };

  const closeTaskModal = () => {
    setSelectedTaskId(null);
    setActivityTarget(null);
  };

  const selectedTaskObj =
    boardData?.columns && selectedTaskId
      ? Object.values(boardData.columns)
          .flat()
          .find((task) => Number(task.id) === Number(selectedTaskId))
      : listData?.content?.find(
          (task) => Number(task.id) === Number(selectedTaskId)
        );

  if (projectLoading && !project) {
    return (
      <div className="project-detail-page">
        <Skeleton height="110px" style={{ borderRadius: '16px' }} />
        <Skeleton height="42px" style={{ borderRadius: '8px' }} />
        <div style={{ display: 'flex', gap: '16px', flex: 1 }}>
          <Skeleton style={{ flex: 1, borderRadius: '14px' }} />
          <Skeleton style={{ flex: 1, borderRadius: '14px' }} />
          <Skeleton style={{ flex: 1, borderRadius: '14px' }} />
        </div>
      </div>
    );
  }

  if (projectError) {
    return (
      <div className="project-detail-page">
        <ErrorState
          title="Project Not Found"
          message={
            projectError.status === 404
              ? 'This project does not exist or you do not have permission to view it.'
              : projectError.message
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

  return (
    <div className="project-detail-page">
      <header className="project-header-card">
        <div className="project-header-top">
          <div className="project-header-title-area">
            <div className="project-header-title-row">
              <h1 className="project-title-text">{project.name}</h1>
              <StatusBadge status={project.status} />
            </div>

            {project.description && (
              <p className="project-header-desc">{project.description}</p>
            )}
          </div>

          <div style={{ width: '220px' }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                fontSize: '0.8rem',
                color: 'var(--text-secondary)',
                marginBottom: '4px'
              }}
            >
              <span>Progress</span>
              <span>
                {project.completedTasks ?? 0} of {project.totalTasks ?? 0} done
              </span>
            </div>
            <ProgressBar value={project.progressPercent ?? 0} />
          </div>
        </div>

        <div className="project-header-meta">
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Calendar size={15} />
            {project.startDate
              ? `Starts ${formatDate(project.startDate)}`
              : 'No start date'}
          </span>

          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Calendar size={15} />
            {project.dueDate
              ? `Due ${formatDate(project.dueDate)}`
              : 'No due date'}
          </span>

          <span>Created by: {project.createdByName || 'Team Member'}</span>
        </div>

        {isArchived && (
          <div className="project-archived-banner" role="alert">
            <AlertTriangle size={18} />
            <span>
              This project is archived. All tasks and board positions are read-only.
              Restore it from the workspace settings to make updates.
            </span>
          </div>
        )}
      </header>

      <div className="project-toolbar">
        {viewMode !== 'analytics' && viewMode !== 'activity' && (
          <div className="project-filters">
            <input
              type="text"
              className="filter-input"
              placeholder="Search tasks..."
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />

            <select
              className="filter-select"
              value={assigneeFilter}
              onChange={(event) => setAssigneeFilter(event.target.value)}
            >
              <option value="">All Assignees</option>
              <option value="unassigned">Unassigned</option>
              {members.map((member) => (
                <option key={member.userId} value={String(member.userId)}>
                  {member.name}
                </option>
              ))}
            </select>

            <select
              className="filter-select"
              value={priorityFilter}
              onChange={(event) => setPriorityFilter(event.target.value)}
            >
              <option value="">All Priorities</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </select>

            <input
              type="date"
              className="filter-input"
              title="Due from"
              value={dueFrom}
              onChange={(event) => setDueFrom(event.target.value)}
            />

            <input
              type="date"
              className="filter-input"
              title="Due to"
              value={dueTo}
              onChange={(event) => setDueTo(event.target.value)}
            />
          </div>
        )}

        <div className="view-toggle-group" aria-label="Project view">
          <button
            type="button"
            className={`view-toggle-btn ${viewMode === 'board' ? 'active' : ''}`}
            onClick={() => setViewMode('board')}
          >
            <Kanban size={15} />
            Board
          </button>

          <button
            type="button"
            className={`view-toggle-btn ${viewMode === 'list' ? 'active' : ''}`}
            onClick={() => setViewMode('list')}
          >
            <List size={15} />
            List
          </button>

          <button
            type="button"
            className={`view-toggle-btn ${viewMode === 'analytics' ? 'active' : ''}`}
            onClick={() => setViewMode('analytics')}
          >
            <BarChart3 size={15} />
            Analytics
          </button>

          <button
            type="button"
            className={`view-toggle-btn ${viewMode === 'activity' ? 'active' : ''}`}
            onClick={() => setViewMode('activity')}
          >
            <Activity size={15} />
            Activity
          </button>
        </div>
      </div>

      {viewMode === 'analytics' ? (
        <ProjectAnalytics projectId={Number(projectId)} />
      ) : viewMode === 'activity' ? (
        <ProjectActivity
          projectId={Number(projectId)}
          members={members}
          onOpenActivity={handleOpenActivity}
        />
      ) : viewMode === 'board' ? (
        boardLoading && !boardData ? (
          <div style={{ display: 'flex', gap: '18px', flex: 1 }}>
            <Skeleton style={{ flex: 1, borderRadius: '14px' }} />
            <Skeleton style={{ flex: 1, borderRadius: '14px' }} />
            <Skeleton style={{ flex: 1, borderRadius: '14px' }} />
            <Skeleton style={{ flex: 1, borderRadius: '14px' }} />
          </div>
        ) : boardError ? (
          <ErrorState
            title="Could not load the board"
            message={boardError.message}
            onRetry={reloadBoard}
          />
        ) : (
          <KanbanBoard
            boardData={boardData}
            projectId={project.id}
            onOpenTask={openTaskFromBoardOrList}
            canMoveTask={userCanMove}
            isArchived={isArchived}
            onReloadBoard={refreshAll}
          />
        )
      ) : listLoading && !listData ? (
        <Skeleton style={{ flex: 1, borderRadius: '14px' }} />
      ) : (
        <TaskListView
          pagedData={listData}
          page={page}
          size={20}
          sortBy={sortBy}
          direction={direction}
          onSortChange={(column, sortDirection) => {
            setSortBy(column);
            setDirection(sortDirection);
          }}
          onPageChange={(newPage) => setPage(newPage)}
          onOpenTask={openTaskFromBoardOrList}
        />
      )}

      {selectedTaskId && (
        <TaskModal
          key={`${selectedTaskId}-${activityTarget?.eventId ?? activityTarget?.commentId ?? 'task'}`}
          isOpen={Boolean(selectedTaskId)}
          onClose={closeTaskModal}
          taskId={selectedTaskId}
          navigation={
            Number(activityTarget?.taskId) === Number(selectedTaskId)
              ? activityTarget
              : null
          }
          workspaceRole={workspace?.myRole}
          workspaceMembers={members}
          canEdit={
            selectedTaskObj
              ? canEditTask({
                  myRole: workspace?.myRole,
                  task: selectedTaskObj,
                  currentUserId: user?.id,
                  isProjectArchived: isArchived
                })
              : false
          }
          canAssign={
            selectedTaskObj
              ? canAssignTask({
                  myRole: workspace?.myRole,
                  task: selectedTaskObj,
                  currentUserId: user?.id,
                  isProjectArchived: isArchived
                })
              : false
          }
          canDelete={
            selectedTaskObj
              ? canDeleteTask({
                  myRole: workspace?.myRole,
                  task: selectedTaskObj,
                  currentUserId: user?.id,
                  isProjectArchived: isArchived
                })
              : false
          }
          isArchived={isArchived}
          onTaskUpdated={refreshAll}
          onTaskDeleted={refreshAll}
        />
      )}
    </div>
  );
}