package com.teamcanvas.invitation.dto;

import com.teamcanvas.invitation.InvitationStatus;
import com.teamcanvas.invitation.WorkspaceInvitation;
import com.teamcanvas.workspace.WorkspaceRole;
import java.time.Instant;

public record InvitationResponse(
        Long id,
        Long workspaceId,
        String workspaceName,
        String inviteeName,
        String inviteeEmail,
        String invitedByName,
        WorkspaceRole role,
        InvitationStatus status,
        Instant expiresAt,
        Instant createdAt) {

    public static InvitationResponse from(WorkspaceInvitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getWorkspace().getId(),
                invitation.getWorkspace().getName(),
                invitation.getInvitee().getName(),
                invitation.getInvitee().getEmail(),
                invitation.getInvitedBy().getName(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt());
    }
}
