package com.teamcanvas.invitation.dto;

import com.teamcanvas.workspace.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** "role" is optional and defaults to MEMBER. */
public record CreateInvitationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        WorkspaceRole role) {
}
