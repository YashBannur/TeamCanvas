package com.teamcanvas.workspace;

import com.teamcanvas.security.UserPrincipal;
import com.teamcanvas.workspace.dto.ChangeRoleRequest;
import com.teamcanvas.workspace.dto.CreateWorkspaceRequest;
import com.teamcanvas.workspace.dto.MemberResponse;
import com.teamcanvas.workspace.dto.UpdateWorkspaceRequest;
import com.teamcanvas.workspace.dto.WorkspaceResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Thin controller: the caller's id always comes from the verified JWT, never from the request. */
@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody CreateWorkspaceRequest request) {
        return workspaceService.create(principal.getId(), request);
    }

    @GetMapping
    public List<WorkspaceResponse> listMine(@AuthenticationPrincipal UserPrincipal principal) {
        return workspaceService.listMine(principal.getId());
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse getOne(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long workspaceId) {
        return workspaceService.getOne(workspaceId, principal.getId());
    }

    @PatchMapping("/{workspaceId}")
    public WorkspaceResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long workspaceId,
                                    @Valid @RequestBody UpdateWorkspaceRequest request) {
        return workspaceService.update(workspaceId, principal.getId(), request);
    }

    @DeleteMapping("/{workspaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long workspaceId) {
        workspaceService.delete(workspaceId, principal.getId());
    }

    @GetMapping("/{workspaceId}/members")
    public List<MemberResponse> listMembers(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long workspaceId) {
        return workspaceService.listMembers(workspaceId, principal.getId());
    }

    @PatchMapping("/{workspaceId}/members/{userId}")
    public MemberResponse changeRole(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long workspaceId,
                                     @PathVariable Long userId,
                                     @Valid @RequestBody ChangeRoleRequest request) {
        return workspaceService.changeRole(workspaceId, principal.getId(), userId, request);
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal UserPrincipal principal,
                             @PathVariable Long workspaceId,
                             @PathVariable Long userId) {
        workspaceService.removeMember(workspaceId, principal.getId(), userId);
    }

    @PostMapping("/{workspaceId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long workspaceId) {
        workspaceService.leave(workspaceId, principal.getId());
    }
}
