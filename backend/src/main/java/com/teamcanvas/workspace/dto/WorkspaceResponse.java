package com.teamcanvas.workspace.dto;

import com.teamcanvas.workspace.Workspace;
import com.teamcanvas.workspace.WorkspaceRole;
import java.time.Instant;

/** A workspace as seen by one particular caller (myRole is the caller's own role). */
public record WorkspaceResponse(Long id, String name, String description, WorkspaceRole myRole,
                                long memberCount, Instant createdAt) {

    public static WorkspaceResponse of(Workspace workspace, WorkspaceRole myRole, long memberCount) {
        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getDescription(),
                myRole, memberCount, workspace.getCreatedAt());
    }
}
