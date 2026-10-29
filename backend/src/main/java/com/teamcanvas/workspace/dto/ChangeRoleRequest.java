package com.teamcanvas.workspace.dto;

import com.teamcanvas.workspace.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull(message = "Role is required") WorkspaceRole role) {
}
