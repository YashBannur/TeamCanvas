package com.teamcanvas.security;

import com.teamcanvas.common.exception.BadRequestException;
import com.teamcanvas.common.exception.ForbiddenException;
import com.teamcanvas.common.exception.ResourceNotFoundException;
import com.teamcanvas.task.Task;
import com.teamcanvas.task.TaskStatus;
import com.teamcanvas.workspace.WorkspaceMember;
import com.teamcanvas.workspace.WorkspaceMemberRepository;
import com.teamcanvas.workspace.WorkspaceRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PermissionService {

    private static final String WORKSPACE_NOT_FOUND = "Workspace not found";

    private final WorkspaceMemberRepository memberRepository;

    public PermissionService(WorkspaceMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public WorkspaceMember requireMember(Long workspaceId, Long userId) {
        return requireMember(workspaceId, userId, WORKSPACE_NOT_FOUND);
    }

    public WorkspaceMember requireMember(
            Long workspaceId, Long userId, String notFoundMessage) {
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
    }

    public WorkspaceMember requireAdminOrOwner(Long workspaceId, Long userId) {
        return requireAdminOrOwner(workspaceId, userId, WORKSPACE_NOT_FOUND);
    }

    public WorkspaceMember requireAdminOrOwner(
            Long workspaceId, Long userId, String notFoundMessage) {
        WorkspaceMember member = requireMember(workspaceId, userId, notFoundMessage);
        if (member.getRole() == WorkspaceRole.MEMBER) {
            throw new ForbiddenException(
                    "Only workspace owners and admins can do this");
        }
        return member;
    }

    public WorkspaceMember requireOwner(Long workspaceId, Long userId) {
        WorkspaceMember member = requireMember(workspaceId, userId);
        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException(
                    "Only the workspace owner can do this");
        }
        return member;
    }

    /** Only workspace owners and admins may edit task fields or change assignees. */
    public void requireCanEditTask(WorkspaceMember member, Task task) {
        if (member.getRole() != WorkspaceRole.MEMBER) {
            return;
        }

        boolean isCreator =
                member.getUser().getId().equals(task.getReporter().getId());

        if (!isCreator) {
            throw new ForbiddenException(
                    "Only the task creator, workspace owner, or workspace admin can edit task details");
        }
    }

    /**
     * A member may move tasks they created, tasks assigned to them, or unassigned tasks.
     * Every task must pass through In Review before it can become Done. Only an owner
     * or admin may approve a task by moving it from In Review to Done.
     */
    public void requireCanMoveTask(
            WorkspaceMember member, Task task, TaskStatus targetStatus) {
        boolean isMember = member.getRole() == WorkspaceRole.MEMBER;

        if (isMember) {
            Long userId = member.getUser().getId();
            boolean unassigned = task.getAssignee() == null;
            boolean isAssignee =
                    !unassigned && userId.equals(task.getAssignee().getId());
            boolean isCreator = userId.equals(task.getReporter().getId());

            if (!(unassigned || isAssignee || isCreator)) {
                throw new ForbiddenException(
                        "You can only move tasks you created, tasks assigned to you, "
                                + "or unassigned tasks");
            }
        }

        if (targetStatus == TaskStatus.DONE
                && task.getStatus() != TaskStatus.IN_REVIEW) {
            throw new BadRequestException(
                    "A task must move to In Review before it can be marked Done");
        }

        if (isMember
                && targetStatus == TaskStatus.DONE
                && task.getStatus() == TaskStatus.IN_REVIEW) {
            throw new ForbiddenException(
                    "Only workspace owners and admins can approve tasks in In Review");
        }
    }

    /** Owners and admins may delete any task; a member may delete only a task they created. */
    public void requireCanDeleteTask(WorkspaceMember member, Task task) {
        if (member.getRole() != WorkspaceRole.MEMBER) {
            return;
        }
        if (!member.getUser().getId().equals(task.getReporter().getId())) {
            throw new ForbiddenException(
                    "You can only delete tasks you created");
        }
    }
}