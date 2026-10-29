package com.teamcanvas.invitation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamcanvas.support.ApiTestSupport;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Invitation rules: who may invite, duplicates, who may answer, expiry, cancelling. */
class InvitationIntegrationTest extends ApiTestSupport {

    private ResultActions invite(TestUser by, long workspaceId, String email, String role) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        if (role != null) {
            body.put("role", role);
        }
        return post(by, "/api/workspaces/" + workspaceId + "/invitations", body);
    }

    @Test
    void invite_createsAPendingInvitationWithAnExpiry() throws Exception {
        TestUser owner = newUser();
        TestUser invitee = newUser();
        long workspaceId = createWorkspace(owner);

        invite(owner, workspaceId, invitee.email(), "ADMIN")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.inviteeEmail").value(invitee.email()))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void invite_withoutARole_defaultsToMember() throws Exception {
        TestUser owner = newUser();
        TestUser invitee = newUser();
        long workspaceId = createWorkspace(owner);

        invite(owner, workspaceId, invitee.email(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void invite_whileOneIsPending_returns409() throws Exception {
        TestUser owner = newUser();
        TestUser invitee = newUser();
        long workspaceId = createWorkspace(owner);
        invite(owner, workspaceId, invitee.email(), "MEMBER").andExpect(status().isCreated());

        invite(owner, workspaceId, invitee.email(), "MEMBER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This user already has a pending invitation"));
    }

    @Test
    void invite_invalidTargets_areRejected() throws Exception {
        TestUser owner = newUser();
        TestUser existingMember = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, existingMember, "MEMBER");

        invite(owner, workspaceId, "nobody-" + System.nanoTime() + "@example.com", "MEMBER")
                .andExpect(status().isNotFound());
        invite(owner, workspaceId, owner.email(), "MEMBER").andExpect(status().isBadRequest());
        invite(owner, workspaceId, existingMember.email(), "MEMBER").andExpect(status().isConflict());
        invite(owner, workspaceId, "not-an-email", "MEMBER").andExpect(status().isBadRequest());

        TestUser someoneNew = newUser();
        invite(owner, workspaceId, someoneNew.email(), "OWNER").andExpect(status().isBadRequest());
    }

    @Test
    void invite_permissions_followTheRoles() throws Exception {
        TestUser owner = newUser();
        TestUser admin = newUser();
        TestUser member = newUser();
        TestUser target = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, admin, "ADMIN");
        addMember(owner, workspaceId, member, "MEMBER");

        invite(member, workspaceId, target.email(), "MEMBER").andExpect(status().isForbidden());
        invite(admin, workspaceId, target.email(), "ADMIN")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the workspace owner can invite admins"));
        invite(admin, workspaceId, target.email(), "MEMBER").andExpect(status().isCreated());
    }

    @Test
    void onlyTheInvitee_canAcceptOrReject() throws Exception {
        TestUser owner = newUser();
        TestUser invitee = newUser();
        TestUser stranger = newUser();
        long workspaceId = createWorkspace(owner);
        long invitationId = idOf(invite(owner, workspaceId, invitee.email(), "MEMBER")
                .andExpect(status().isCreated()));

        post(stranger, "/api/invitations/" + invitationId + "/accept", null).andExpect(status().isNotFound());
        post(stranger, "/api/invitations/" + invitationId + "/reject", null).andExpect(status().isNotFound());
        get(invitee, "/api/workspaces/" + workspaceId).andExpect(status().isNotFound());   // not a member yet
    }

    @Test
    void accept_createsAMembershipWithTheInvitedRole() throws Exception {
        TestUser owner = newUser();
        TestUser invitee = newUser();
        long workspaceId = createWorkspace(owner);
        long invitationId = idOf(invite(owner, workspaceId, invitee.email(), "ADMIN")
                .andExpect(status().isCreated()));

        post(invitee, "/api/invitations/" + invitationId + "/accept", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("ADMIN"))
                .andExpect(jsonPath("$.memberCount").value(2));

        get(invitee, "/api/workspaces/" + workspaceId).andExpect(status().isOk());
    }

    @Test
    void accept_twice_returns409() throws Exception {
        TestUser owner = newUser();
        TestUser invitee = newUser();
        long workspaceId = createWorkspace(owner);
        long invitationId = idOf(invite(owner, workspaceId, invitee.email(), "MEMBER")
                .andExpect(status().isCreated()));

        post(invitee, "/api/invitations/" + invitationId + "/accept", null).andExpect(status().isOk());
        post(invitee, "/api/invitations/" + invitationId + "/accept", null).andExpect(status().isConflict());
    }

    @Test
    void reject_closesTheInvitation_andAllowsANewOne() throws Exception {
        TestUser owner = newUser();
        TestUser invitee = newUser();
        long workspaceId = createWorkspace(owner);
        long invitationId = idOf(invite(owner, workspaceId, invitee.email(), "MEMBER")
                .andExpect(status().isCreated()));

        post(invitee, "/api/invitations/" + invitationId + "/reject", null).andExpect(status().isNoContent());
        post(invitee, "/api/invitations/" + invitationId + "/accept", null).andExpect(status().isConflict());

        invite(owner, workspaceId, invitee.email(), "MEMBER").andExpect(status().isCreated());
    }

    @Test
    void cancel_preventsAcceptance() throws Exception {
        TestUser owner = newUser();
        TestUser invitee = newUser();
        long workspaceId = createWorkspace(owner);
        long invitationId = idOf(invite(owner, workspaceId, invitee.email(), "MEMBER")
                .andExpect(status().isCreated()));

        delete(owner, "/api/workspaces/" + workspaceId + "/invitations/" + invitationId)
                .andExpect(status().isNoContent());
        post(invitee, "/api/invitations/" + invitationId + "/accept", null).andExpect(status().isConflict());
    }

    @Test
    void expired_cannotBeAccepted_andDoesNotBlockANewInvitation() throws Exception {
        TestUser owner = newUser();
        TestUser invitee = newUser();
        long workspaceId = createWorkspace(owner);
        long oldId = idOf(invite(owner, workspaceId, invitee.email(), "MEMBER").andExpect(status().isCreated()));

        // Nobody can wait 7 days in a test, so push the expiry far into the past.
        jdbcTemplate.update("update workspace_invitations set expires_at = '2000-01-01 00:00:00' where id = ?", oldId);

        get(invitee, "/api/invitations").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        post(invitee, "/api/invitations/" + oldId + "/accept", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This invitation has expired"));

        invite(owner, workspaceId, invitee.email(), "MEMBER").andExpect(status().isCreated());

        String oldStatus = jdbcTemplate.queryForObject(
                "select status from workspace_invitations where id = ?", String.class, oldId);
        assertEquals("EXPIRED", oldStatus);
    }

    @Test
    void listMine_returnsOnlyTheCallersInvitations() throws Exception {
        TestUser owner = newUser();
        TestUser first = newUser();
        TestUser second = newUser();
        long workspaceId = createWorkspace(owner);
        invite(owner, workspaceId, first.email(), "MEMBER").andExpect(status().isCreated());

        get(first, "/api/invitations")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].workspaceId").value(workspaceId));
        get(second, "/api/invitations").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void workspaceInvitationList_isForOwnersAndAdminsOnly() throws Exception {
        TestUser owner = newUser();
        TestUser admin = newUser();
        TestUser member = newUser();
        TestUser target = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, admin, "ADMIN");
        addMember(owner, workspaceId, member, "MEMBER");
        invite(owner, workspaceId, target.email(), "MEMBER").andExpect(status().isCreated());
        String url = "/api/workspaces/" + workspaceId + "/invitations";

        get(member, url).andExpect(status().isForbidden());
        get(admin, url).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        get(owner, url).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
}