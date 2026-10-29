package com.teamcanvas.invitation;

import com.teamcanvas.activity.ActivityLogWriter;
import com.teamcanvas.common.exception.BadRequestException;
import com.teamcanvas.common.exception.ConflictException;
import com.teamcanvas.common.exception.ForbiddenException;
import com.teamcanvas.common.exception.ResourceNotFoundException;
import com.teamcanvas.common.util.Emails;
import com.teamcanvas.invitation.dto.CreateInvitationRequest;
import com.teamcanvas.invitation.dto.InvitationResponse;
import com.teamcanvas.notification.NotificationService;
import com.teamcanvas.security.PermissionService;
import com.teamcanvas.user.User;
import com.teamcanvas.user.UserRepository;
import com.teamcanvas.workspace.WorkspaceMember;
import com.teamcanvas.workspace.WorkspaceMemberRepository;
import com.teamcanvas.workspace.WorkspaceRole;
import com.teamcanvas.workspace.dto.WorkspaceResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationService {

    static final Duration VALIDITY = Duration.ofDays(7);

    private final InvitationRepository invitationRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ActivityLogWriter activityLogWriter;
    private final NotificationService notificationService;

    public InvitationService(InvitationRepository invitationRepository,
                             WorkspaceMemberRepository memberRepository,
                             UserRepository userRepository,
                             PermissionService permissionService,
                             ActivityLogWriter activityLogWriter,
                             NotificationService notificationService) {
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.activityLogWriter = activityLogWriter;
        this.notificationService = notificationService;
    }

    @Transactional
    public InvitationResponse invite(Long workspaceId, Long actorId, CreateInvitationRequest request) {
        WorkspaceMember actor = permissionService.requireAdminOrOwner(workspaceId, actorId);

        WorkspaceRole role = request.role() == null ? WorkspaceRole.MEMBER : request.role();
        if (role == WorkspaceRole.OWNER) {
            throw new BadRequestException("Ownership cannot be granted through an invitation");
        }
        if (role == WorkspaceRole.ADMIN && actor.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("Only the workspace owner can invite admins");
        }

        User invitee = userRepository.findByEmail(Emails.normalize(request.email()))
                .orElseThrow(() -> new ResourceNotFoundException("No registered user found with this email"));
        if (invitee.getId().equals(actorId)) {
            throw new BadRequestException("You cannot invite yourself");
        }
        if (memberRepository.findByWorkspaceIdAndUserId(workspaceId, invitee.getId()).isPresent()) {
            throw new ConflictException("This user is already a member of the workspace");
        }

        Instant now = Instant.now();
        Optional<WorkspaceInvitation> existing = invitationRepository
                .findByWorkspaceIdAndInviteeIdAndStatus(workspaceId, invitee.getId(), InvitationStatus.PENDING);
        if (existing.isPresent()) {
            WorkspaceInvitation pending = existing.get();
            if (!pending.isExpired(now)) {
                throw new ConflictException("This user already has a pending invitation");
            }
            pending.markExpired();
            invitationRepository.saveAndFlush(pending);
        }

        WorkspaceInvitation invitation = invitationRepository.save(
                new WorkspaceInvitation(actor.getWorkspace(), invitee, actor.getUser(), role, now.plus(VALIDITY)));

        activityLogWriter.record(
                workspaceId, null, actorId,
                ActivityLogWriter.INVITATION_SENT, "INVITATION", invitation.getId(),
                "role=" + role);

        notificationService.invitationReceived(invitation);

        return InvitationResponse.from(invitation);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listForWorkspace(Long workspaceId, Long actorId) {
        permissionService.requireAdminOrOwner(workspaceId, actorId);
        return invitationRepository
                .findActiveForWorkspace(workspaceId, InvitationStatus.PENDING, Instant.now()).stream()
                .map(InvitationResponse::from)
                .toList();
    }

    @Transactional
    public void cancel(Long workspaceId, Long invitationId, Long actorId) {
        permissionService.requireAdminOrOwner(workspaceId, actorId);
        WorkspaceInvitation invitation = invitationRepository.findByIdAndWorkspaceId(invitationId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        if (!invitation.isPending()) {
            throw new ConflictException("Only pending invitations can be cancelled");
        }
        invitation.cancel(Instant.now());
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listMine(Long userId) {
        return invitationRepository.findActiveForInvitee(userId, InvitationStatus.PENDING, Instant.now()).stream()
                .map(InvitationResponse::from)
                .toList();
    }

    @Transactional
    public WorkspaceResponse accept(Long invitationId, Long userId) {
        WorkspaceInvitation invitation = loadOwnOpenInvitation(invitationId, userId);

        Long workspaceId = invitation.getWorkspace().getId();
        if (memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId).isPresent()) {
            throw new ConflictException("You are already a member of this workspace");
        }

        memberRepository.save(new WorkspaceMember(invitation.getWorkspace(), invitation.getInvitee(),
                invitation.getRole()));
        invitation.accept(Instant.now());

        activityLogWriter.record(
                workspaceId, null, userId,
                ActivityLogWriter.MEMBER_ADDED, "MEMBER", userId,
                "role=" + invitation.getRole());

        long memberCount = memberRepository.countByWorkspaceId(workspaceId);
        return WorkspaceResponse.of(invitation.getWorkspace(), invitation.getRole(), memberCount);
    }

    @Transactional
    public void reject(Long invitationId, Long userId) {
        WorkspaceInvitation invitation = loadOwnOpenInvitation(invitationId, userId);
        invitation.reject(Instant.now());
    }

    private WorkspaceInvitation loadOwnOpenInvitation(Long invitationId, Long userId) {
        WorkspaceInvitation invitation = invitationRepository.findByIdAndInviteeId(invitationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        if (!invitation.isPending()) {
            throw new ConflictException("This invitation has already been answered or cancelled");
        }
        if (invitation.isExpired(Instant.now())) {
            throw new BadRequestException("This invitation has expired");
        }
        return invitation;
    }
}