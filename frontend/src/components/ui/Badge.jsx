import { ROLE_LABELS, WORKSPACE_ROLES } from '../../constants/roles';
import { TASK_PRIORITY_LABELS, TASK_PRIORITIES } from '../../constants/taskPriority';
import { TASK_STATUS_LABELS, TASK_STATUS } from '../../constants/taskStatus';
import { PROJECT_STATUS_LABELS, PROJECT_STATUS } from '../../constants/projectStatus';

export function RoleBadge({ role }) {
  const roleClassMap = {
    [WORKSPACE_ROLES.OWNER]: 'tc-badge--role-owner',
    [WORKSPACE_ROLES.ADMIN]: 'tc-badge--role-admin',
    [WORKSPACE_ROLES.MEMBER]: 'tc-badge--role-member'
  };

  return (
    <span className={`tc-badge ${roleClassMap[role] || 'tc-badge--role-member'}`}>
      {ROLE_LABELS[role] || role}
    </span>
  );
}

export function PriorityBadge({ priority }) {
  const priorityClassMap = {
    [TASK_PRIORITIES.LOW]: 'tc-badge--priority-low',
    [TASK_PRIORITIES.MEDIUM]: 'tc-badge--priority-medium',
    [TASK_PRIORITIES.HIGH]: 'tc-badge--priority-high',
    [TASK_PRIORITIES.URGENT]: 'tc-badge--priority-urgent'
  };

  return (
    <span className={`tc-badge ${priorityClassMap[priority] || 'tc-badge--priority-low'}`}>
      {TASK_PRIORITY_LABELS[priority] || priority}
    </span>
  );
}

export function StatusBadge({ status }) {
  const statusClassMap = {
    [TASK_STATUS.TODO]: 'tc-badge--status-todo',
    [TASK_STATUS.IN_PROGRESS]: 'tc-badge--status-in-progress',
    [TASK_STATUS.IN_REVIEW]: 'tc-badge--status-in-review',
    [TASK_STATUS.DONE]: 'tc-badge--status-done',
    [PROJECT_STATUS.ACTIVE]: 'tc-badge--status-active',
    [PROJECT_STATUS.ARCHIVED]: 'tc-badge--status-archived'
  };

  const label = TASK_STATUS_LABELS[status] || PROJECT_STATUS_LABELS[status] || status;

  return (
    <span className={`tc-badge ${statusClassMap[status] || 'tc-badge--status-todo'}`}>
      {label}
    </span>
  );
}