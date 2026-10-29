import { WORKSPACE_ROLES } from '../constants/roles';

export function canManageWorkspace(role) {
  return role === WORKSPACE_ROLES.OWNER || role === WORKSPACE_ROLES.ADMIN;
}

export function canDeleteWorkspace(role) {
  return role === WORKSPACE_ROLES.OWNER;
}

export function canInviteRole(myRole, targetRole) {
  if (myRole === WORKSPACE_ROLES.OWNER) {
    return targetRole === WORKSPACE_ROLES.ADMIN || targetRole === WORKSPACE_ROLES.MEMBER;
  }
  if (myRole === WORKSPACE_ROLES.ADMIN) {
    return targetRole === WORKSPACE_ROLES.MEMBER;
  }
  return false;
}

export function canChangeMemberRole(myRole) {
  return myRole === WORKSPACE_ROLES.OWNER;
}

export function canRemoveMember(myRole, targetRole, isSelf) {
  if (isSelf) return false;
  if (myRole === WORKSPACE_ROLES.OWNER) return true;
  if (myRole === WORKSPACE_ROLES.ADMIN) {
    return targetRole === WORKSPACE_ROLES.MEMBER;
  }
  return false;
}

export function canLeaveWorkspace(myRole) {
  return myRole === WORKSPACE_ROLES.ADMIN || myRole === WORKSPACE_ROLES.MEMBER;
}

export function canManageProjects(role) {
  return role === WORKSPACE_ROLES.OWNER || role === WORKSPACE_ROLES.ADMIN;
}

function isOwnerOrAdmin(role) {
  return role === WORKSPACE_ROLES.OWNER || role === WORKSPACE_ROLES.ADMIN;
}

function isTaskCreator(task, currentUserId) {
  return (
    task &&
    currentUserId != null &&
    Number(task.reporterId) === Number(currentUserId)
  );
}

function canMemberMoveTask(task, currentUserId) {
  if (!task || currentUserId == null) return false;

  const isCreator = isTaskCreator(task, currentUserId);
  const isAssignee =
    task.assigneeId != null &&
    Number(task.assigneeId) === Number(currentUserId);
  const isUnassigned = task.assigneeId == null;

  return isCreator || isAssignee || isUnassigned;
}

export function canEditTask({ myRole, task, currentUserId, isProjectArchived }) {
  if (isProjectArchived) return false;
  return isOwnerOrAdmin(myRole) || isTaskCreator(task, currentUserId);
}

export function canAssignTask({ myRole, task, currentUserId, isProjectArchived }) {
  if (isProjectArchived) return false;
  return isOwnerOrAdmin(myRole) || isTaskCreator(task, currentUserId);
}

export function canMoveTask({ myRole, task, currentUserId, isProjectArchived }) {
  if (isProjectArchived) return false;
  return isOwnerOrAdmin(myRole) || canMemberMoveTask(task, currentUserId);
}

export function canDeleteTask({ myRole, task, currentUserId, isProjectArchived }) {
  if (isProjectArchived) return false;
  return isOwnerOrAdmin(myRole) || isTaskCreator(task, currentUserId);
}