package com.teamcanvas.workspace.dto;

import com.teamcanvas.workspace.WorkspaceMember;
import com.teamcanvas.workspace.WorkspaceRole;
import java.time.Instant;

public record MemberResponse(Long userId, String name, String email, WorkspaceRole role, Instant joinedAt) {

    public static MemberResponse from(WorkspaceMember member) {
        return new MemberResponse(member.getUser().getId(), member.getUser().getName(),
                member.getUser().getEmail(), member.getRole(), member.getJoinedAt());
    }
}
