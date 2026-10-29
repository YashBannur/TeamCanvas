import { describe, it, expect } from 'vitest';
import {
  canManageWorkspace,
  canDeleteWorkspace,
  canInviteRole,
  canAssignTask,
  canEditTask,
  canMoveTask,
  canDeleteTask
} from '../utils/permissions';
import { WORKSPACE_ROLES } from '../constants/roles';

describe('Permissions Logic', () => {
  it('identifies workspace management permissions correctly', () => {
    expect(canManageWorkspace(WORKSPACE_ROLES.OWNER)).toBe(true);
    expect(canManageWorkspace(WORKSPACE_ROLES.ADMIN)).toBe(true);
    expect(canManageWorkspace(WORKSPACE_ROLES.MEMBER)).toBe(false);

    expect(canDeleteWorkspace(WORKSPACE_ROLES.OWNER)).toBe(true);
    expect(canDeleteWorkspace(WORKSPACE_ROLES.ADMIN)).toBe(false);
  });

  it('enforces invitation role constraints', () => {
    expect(canInviteRole(WORKSPACE_ROLES.OWNER, WORKSPACE_ROLES.ADMIN)).toBe(true);
    expect(canInviteRole(WORKSPACE_ROLES.OWNER, WORKSPACE_ROLES.MEMBER)).toBe(true);
    expect(canInviteRole(WORKSPACE_ROLES.ADMIN, WORKSPACE_ROLES.ADMIN)).toBe(false);
    expect(canInviteRole(WORKSPACE_ROLES.ADMIN, WORKSPACE_ROLES.MEMBER)).toBe(true);
  });

  it('allows owners and admins to edit, assign, and move any active-project task', () => {
    const task = { reporterId: 10, assigneeId: 20 };

    for (const role of [WORKSPACE_ROLES.OWNER, WORKSPACE_ROLES.ADMIN]) {
      const args = { myRole: role, task, currentUserId: 99 };

      expect(canEditTask(args)).toBe(true);
      expect(canAssignTask(args)).toBe(true);
      expect(canMoveTask(args)).toBe(true);
    }
  });

  it('allows the task creator to edit, assign, and move their task', () => {
    const args = {
      myRole: WORKSPACE_ROLES.MEMBER,
      task: { reporterId: 10, assigneeId: 20 },
      currentUserId: 10
    };

    expect(canEditTask(args)).toBe(true);
    expect(canAssignTask(args)).toBe(true);
    expect(canMoveTask(args)).toBe(true);
  });

  it('allows an assignee to move a task but not edit or reassign it', () => {
    const args = {
      myRole: WORKSPACE_ROLES.MEMBER,
      task: { reporterId: 10, assigneeId: 20 },
      currentUserId: 20
    };

    expect(canEditTask(args)).toBe(false);
    expect(canAssignTask(args)).toBe(false);
    expect(canMoveTask(args)).toBe(true);
  });

  it('allows a member to move an unassigned task but not edit or assign it', () => {
    const tasks = [
      { reporterId: 10, assigneeId: null },
      { reporterId: 10 }
    ];

    for (const task of tasks) {
      const args = {
        myRole: WORKSPACE_ROLES.MEMBER,
        task,
        currentUserId: 30
      };

      expect(canEditTask(args)).toBe(false);
      expect(canAssignTask(args)).toBe(false);
      expect(canMoveTask(args)).toBe(true);
    }
  });

  it('blocks a member from changing a task assigned to someone else', () => {
    const args = {
      myRole: WORKSPACE_ROLES.MEMBER,
      task: { reporterId: 10, assigneeId: 20 },
      currentUserId: 30
    };

    expect(canEditTask(args)).toBe(false);
    expect(canAssignTask(args)).toBe(false);
    expect(canMoveTask(args)).toBe(false);
  });

  it('blocks task changes on archived projects for every role', () => {
    const task = { reporterId: 10, assigneeId: 10 };

    for (const role of [
      WORKSPACE_ROLES.OWNER,
      WORKSPACE_ROLES.ADMIN,
      WORKSPACE_ROLES.MEMBER
    ]) {
      const args = {
        myRole: role,
        task,
        currentUserId: 10,
        isProjectArchived: true
      };

      expect(canEditTask(args)).toBe(false);
      expect(canAssignTask(args)).toBe(false);
      expect(canMoveTask(args)).toBe(false);
    }
  });

  it('validates task deletion rights', () => {
    const task = { id: 1, reporterId: 10 };

    expect(canDeleteTask({
      myRole: WORKSPACE_ROLES.OWNER,
      task,
      currentUserId: 99
    })).toBe(true);

    expect(canDeleteTask({
      myRole: WORKSPACE_ROLES.MEMBER,
      task,
      currentUserId: 10
    })).toBe(true);

    expect(canDeleteTask({
      myRole: WORKSPACE_ROLES.MEMBER,
      task,
      currentUserId: 99
    })).toBe(false);
  });
});