package com.teamcanvas.invitation;

import com.teamcanvas.invitation.dto.CreateInvitationRequest;
import com.teamcanvas.invitation.dto.InvitationResponse;
import com.teamcanvas.security.UserPrincipal;
import com.teamcanvas.workspace.dto.WorkspaceResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Thin controller: the caller's id always comes from the verified JWT. */
@RestController
@RequestMapping("/api")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    // ---- owner / admin side

    @PostMapping("/workspaces/{workspaceId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse invite(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long workspaceId,
                                     @Valid @RequestBody CreateInvitationRequest request) {
        return invitationService.invite(workspaceId, principal.getId(), request);
    }

    @GetMapping("/workspaces/{workspaceId}/invitations")
    public List<InvitationResponse> listForWorkspace(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable Long workspaceId) {
        return invitationService.listForWorkspace(workspaceId, principal.getId());
    }

    @DeleteMapping("/workspaces/{workspaceId}/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable Long workspaceId,
                       @PathVariable Long invitationId) {
        invitationService.cancel(workspaceId, invitationId, principal.getId());
    }

    // ---- invited user side

    @GetMapping("/invitations")
    public List<InvitationResponse> listMine(@AuthenticationPrincipal UserPrincipal principal) {
        return invitationService.listMine(principal.getId());
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public WorkspaceResponse accept(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long invitationId) {
        return invitationService.accept(invitationId, principal.getId());
    }

    @PostMapping("/invitations/{invitationId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long invitationId) {
        invitationService.reject(invitationId, principal.getId());
    }
}
