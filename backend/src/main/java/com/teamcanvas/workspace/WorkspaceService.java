package com.teamcanvas.workspace;

import com.teamcanvas.activity.ActivityLogWriter;
import com.teamcanvas.common.exception.BadRequestException;
import com.teamcanvas.common.exception.ForbiddenException;
import com.teamcanvas.common.exception.ResourceNotFoundException;
import com.teamcanvas.security.PermissionService;
import com.teamcanvas.user.User;
import com.teamcanvas.user.UserRepository;
import com.teamcanvas.workspace.dto.ChangeRoleRequest;
import com.teamcanvas.workspace.dto.CreateWorkspaceRequest;
import com.teamcanvas.workspace.dto.MemberResponse;
import com.teamcanvas.workspace.dto.UpdateWorkspaceRequest;
import com.teamcanvas.workspace.dto.WorkspaceResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ActivityLogWriter activityLogWriter;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository memberRepository,
                            UserRepository userRepository,
                            PermissionService permissionService,
                            ActivityLogWriter activityLogWriter) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.activityLogWriter = activityLogWriter;
    }

    @Transactional
    public WorkspaceResponse create(Long userId, CreateWorkspaceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Workspace workspace = workspaceRepository.save(
                new Workspace(request.name().trim(), blankToNull(request.description()), user));
        memberRepository.save(new WorkspaceMember(workspace, user, WorkspaceRole.OWNER));

        activityLogWriter.record(
                workspace.getId(), null, userId,
                ActivityLogWriter.MEMBER_ADDED, "MEMBER", userId,
                "role=" + WorkspaceRole.OWNER);

        return WorkspaceResponse.of(workspace, WorkspaceRole.OWNER, 1);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listMine(Long userId) {
        List<WorkspaceMember> memberships = memberRepository.findAllByUserIdWithWorkspace(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        List<Long> workspaceIds = memberships.stream().map(m -> m.getWorkspace().getId()).toList();
        Map<Long, Long> counts = memberRepository.countByWorkspaceIds(workspaceIds).stream()
                .collect(Collectors.toMap(
                        WorkspaceMemberRepository.MemberCount::getWorkspaceId,
                        WorkspaceMemberRepository.MemberCount::getMemberCount));
        return memberships.stream()
                .map(m -> WorkspaceResponse.of(m.getWorkspace(), m.getRole(),
                        counts.getOrDefault(m.getWorkspace().getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getOne(Long workspaceId, Long userId) {
        WorkspaceMember membership = permissionService.requireMember(workspaceId, userId);
        return toResponse(membership);
    }

    @Transactional
    public WorkspaceResponse update(Long workspaceId, Long userId, UpdateWorkspaceRequest request) {
        WorkspaceMember membership = permissionService.requireAdminOrOwner(workspaceId, userId);
        Workspace workspace = membership.getWorkspace();
        if (request.name() != null) {
            workspace.setName(request.name().trim());
        }
        if (request.description() != null) {
            workspace.setDescription(blankToNull(request.description()));
        }
        return toResponse(membership);
    }

    @Transactional
    public void delete(Long workspaceId, Long userId) {
        WorkspaceMember membership = permissionService.requireOwner(workspaceId, userId);
        workspaceRepository.delete(membership.getWorkspace());
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long workspaceId, Long userId) {
        permissionService.requireMember(workspaceId, userId);
        return memberRepository.findAllByWorkspaceIdWithUser(workspaceId).stream()
                .map(MemberResponse::from)
                .toList();
    }

    @Transactional
    public MemberResponse changeRole(
            Long workspaceId, Long actorId, Long targetUserId, ChangeRoleRequest request) {
        permissionService.requireOwner(workspaceId, actorId);
        WorkspaceMember target = findMember(workspaceId, targetUserId);

        if (target.getRole() == WorkspaceRole.OWNER) {
            throw new ForbiddenException("The owner's role cannot be changed");
        }
        if (request.role() == WorkspaceRole.OWNER) {
            throw new BadRequestException("Ownership transfer is not supported");
        }

        WorkspaceRole previousRole = target.getRole();
        target.setRole(request.role());

        activityLogWriter.record(
                workspaceId, null, actorId,
                ActivityLogWriter.MEMBER_ROLE_CHANGED, "MEMBER", targetUserId,
                previousRole + " -> " + request.role());

        return MemberResponse.from(target);
    }

    @Transactional
    public void removeMember(Long workspaceId, Long actorId, Long targetUserId) {
        WorkspaceMember actor = permissionService.requireAdminOrOwner(workspaceId, actorId);
        WorkspaceMember target = findMember(workspaceId, targetUserId);

        if (targetUserId.equals(actorId)) {
            throw new BadRequestException("Use \"leave\" to leave a workspace");
        }
        if (target.getRole() == WorkspaceRole.OWNER) {
            throw new ForbiddenException("The owner cannot be removed");
        }
        if (actor.getRole() == WorkspaceRole.ADMIN && target.getRole() != WorkspaceRole.MEMBER) {
            throw new ForbiddenException("Admins can only remove regular members");
        }

        activityLogWriter.record(
                workspaceId, null, actorId,
                ActivityLogWriter.MEMBER_REMOVED, "MEMBER", targetUserId,
                "role=" + target.getRole());

        memberRepository.delete(target);
    }

    @Transactional
    public void leave(Long workspaceId, Long userId) {
        WorkspaceMember membership = permissionService.requireMember(workspaceId, userId);
        if (membership.getRole() == WorkspaceRole.OWNER) {
            throw new ForbiddenException("The owner cannot leave the workspace; delete it instead");
        }

        activityLogWriter.record(
                workspaceId, null, userId,
                ActivityLogWriter.MEMBER_REMOVED, "MEMBER", userId,
                "left workspace");

        memberRepository.delete(membership);
    }

    private WorkspaceMember findMember(Long workspaceId, Long userId) {
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }

    private WorkspaceResponse toResponse(WorkspaceMember membership) {
        long memberCount = memberRepository.countByWorkspaceId(membership.getWorkspace().getId());
        return WorkspaceResponse.of(membership.getWorkspace(), membership.getRole(), memberCount);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}