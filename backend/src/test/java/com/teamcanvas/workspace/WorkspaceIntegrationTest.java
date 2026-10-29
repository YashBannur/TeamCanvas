package com.teamcanvas.workspace;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamcanvas.support.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Workspace and membership rules: who may see, change, delete, promote, remove, or leave. */
class WorkspaceIntegrationTest extends ApiTestSupport {

    @Test
    void create_makesTheCallerTheOwner() throws Exception {
        TestUser owner = newUser();

        post(owner, "/api/workspaces", Map.of("name", "Acme", "description", "Our team"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.memberCount").value(1));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        TestUser owner = newUser();

        post(owner, "/api/workspaces", Map.of("name", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void listMine_showsOnlyTheCallersWorkspaces() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        createWorkspace(alice);

        get(alice, "/api/workspaces").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        get(bob, "/api/workspaces").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    /** The core "change the id in the URL" test: an outsider gets the SAME 404 as for a workspace that does not exist. */
    @Test
    void outsider_gets404_forEveryWorkspaceEndpoint() throws Exception {
        TestUser owner = newUser();
        TestUser outsider = newUser();
        long workspaceId = createWorkspace(owner);
        String base = "/api/workspaces/" + workspaceId;

        get(outsider, base).andExpect(status().isNotFound());
        patch(outsider, base, Map.of("name", "Hacked")).andExpect(status().isNotFound());
        delete(outsider, base).andExpect(status().isNotFound());
        get(outsider, base + "/members").andExpect(status().isNotFound());
        get(outsider, base + "/invitations").andExpect(status().isNotFound());
        get(outsider, base + "/projects").andExpect(status().isNotFound());
        post(outsider, base + "/projects", Map.of("name", "Sneaky")).andExpect(status().isNotFound());
        post(outsider, base + "/leave", null).andExpect(status().isNotFound());

        get(outsider, "/api/workspaces/999999999")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Workspace not found"));
    }

    @Test
    void member_canViewButCannotUpdateOrDeleteTheWorkspace() throws Exception {
        TestUser owner = newUser();
        TestUser member = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, member, "MEMBER");
        String base = "/api/workspaces/" + workspaceId;

        get(member, base).andExpect(status().isOk()).andExpect(jsonPath("$.myRole").value("MEMBER"));
        get(member, base + "/members").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        patch(member, base, Map.of("name", "Renamed")).andExpect(status().isForbidden());
        delete(member, base).andExpect(status().isForbidden());
    }

    @Test
    void admin_canUpdate_butOnlyTheOwnerCanDelete() throws Exception {
        TestUser owner = newUser();
        TestUser admin = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, admin, "ADMIN");
        String base = "/api/workspaces/" + workspaceId;

        patch(admin, base, Map.of("name", "Renamed by admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed by admin"));
        delete(admin, base)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the workspace owner can do this"));

        delete(owner, base).andExpect(status().isNoContent());
        get(owner, base).andExpect(status().isNotFound());
    }

    @Test
    void changeRole_isOwnerOnly_andNeverGrantsOwnership() throws Exception {
        TestUser owner = newUser();
        TestUser admin = newUser();
        TestUser member = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, admin, "ADMIN");
        addMember(owner, workspaceId, member, "MEMBER");
        String memberUrl = "/api/workspaces/" + workspaceId + "/members/" + member.id();

        patch(admin, memberUrl, Map.of("role", "ADMIN")).andExpect(status().isForbidden());
        patch(owner, memberUrl, Map.of("role", "OWNER")).andExpect(status().isBadRequest());
        patch(owner, memberUrl, Map.of("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        patch(owner, "/api/workspaces/" + workspaceId + "/members/" + owner.id(), Map.of("role", "MEMBER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeMember_followsTheRoleRules() throws Exception {
        TestUser owner = newUser();
        TestUser admin = newUser();
        TestUser otherAdmin = newUser();
        TestUser member = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, admin, "ADMIN");
        addMember(owner, workspaceId, otherAdmin, "ADMIN");
        addMember(owner, workspaceId, member, "MEMBER");
        String base = "/api/workspaces/" + workspaceId + "/members/";

        delete(member, base + admin.id()).andExpect(status().isForbidden());          // a member cannot remove anyone
        delete(admin, base + otherAdmin.id()).andExpect(status().isForbidden());      // an admin cannot remove an admin
        delete(admin, base + owner.id()).andExpect(status().isForbidden());           // nobody removes the owner
        delete(owner, base + owner.id()).andExpect(status().isBadRequest());          // the owner cannot remove themselves

        delete(admin, base + member.id()).andExpect(status().isNoContent());          // an admin removes a plain member
        get(member, "/api/workspaces/" + workspaceId).andExpect(status().isNotFound());

        delete(owner, base + otherAdmin.id()).andExpect(status().isNoContent());      // the owner removes an admin
    }

    @Test
    void leave_worksForMembers_butNotForTheOwner() throws Exception {
        TestUser owner = newUser();
        TestUser member = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, member, "MEMBER");
        String base = "/api/workspaces/" + workspaceId;

        post(owner, base + "/leave", null).andExpect(status().isForbidden());

        post(member, base + "/leave", null).andExpect(status().isNoContent());
        get(member, base).andExpect(status().isNotFound());
    }
}