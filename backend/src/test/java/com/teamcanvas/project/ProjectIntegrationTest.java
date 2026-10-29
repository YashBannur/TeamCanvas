package com.teamcanvas.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamcanvas.support.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Project rules: permissions, validation, archiving, progress, and cascade delete. */
class ProjectIntegrationTest extends ApiTestSupport {

    @Test
    void create_asOwnerAndAdmin_returns201WithZeroProgress() throws Exception {
        TestUser owner = newUser();
        TestUser admin = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, admin, "ADMIN");
        String url = "/api/workspaces/" + workspaceId + "/projects";

        post(
                        owner,
                        url,
                        Map.of(
                                "name", "Website",
                                "description", "New site",
                                "startDate", "2026-10-20",
                                "dueDate", "2026-12-15"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startDate").value("2026-10-20"))
                .andExpect(jsonPath("$.totalTasks").value(0))
                .andExpect(jsonPath("$.progressPercent").value(0));

        post(admin, url, Map.of("name", "Mobile app"))
                .andExpect(status().isCreated());
    }

    @Test
    void create_asMember_returns403_andAsOutsider404() throws Exception {
        TestUser owner = newUser();
        TestUser member = newUser();
        TestUser outsider = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, member, "MEMBER");
        String url = "/api/workspaces/" + workspaceId + "/projects";

        post(member, url, Map.of("name", "Not allowed"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Only workspace owners and admins can do this"));

        post(outsider, url, Map.of("name", "Sneaky"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_invalidInput_returns400() throws Exception {
        TestUser owner = newUser();
        long workspaceId = createWorkspace(owner);
        String url = "/api/workspaces/" + workspaceId + "/projects";

        post(owner, url, Map.of("name", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        post(owner, url, Map.of(
                        "name", "X",
                        "startDate", "2026-12-01",
                        "dueDate", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Due date cannot be before start date"));

        post(owner, url, Map.of("name", "X", "startDate", "2026-13-45"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_showsActiveProjectsByDefault_andArchivedOnRequest() throws Exception {
        TestUser owner = newUser();
        TestUser member = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, member, "MEMBER");
        long active = createProject(owner, workspaceId);
        long archived = createProject(owner, workspaceId);

        patch(owner, "/api/projects/" + archived, Map.of("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        String url = "/api/workspaces/" + workspaceId + "/projects";

        get(member, url)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(active));

        get(member, url + "?status=ARCHIVED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(archived));

        get(member, url + "?status=FOO")
                .andExpect(status().isBadRequest());
    }

    @Test
    void outsider_gets404_forEveryProjectEndpoint() throws Exception {
        TestUser owner = newUser();
        TestUser outsider = newUser();
        long workspaceId = createWorkspace(owner);
        long projectId = createProject(owner, workspaceId);
        String url = "/api/projects/" + projectId;

        get(outsider, url)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));

        patch(outsider, url, Map.of("name", "Hacked"))
                .andExpect(status().isNotFound());

        delete(outsider, url)
                .andExpect(status().isNotFound());

        get(outsider, "/api/projects/999999999")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void member_canViewButNotChangeOrDelete() throws Exception {
        TestUser owner = newUser();
        TestUser member = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, member, "MEMBER");
        long projectId = createProject(owner, workspaceId);
        String url = "/api/projects/" + projectId;

        get(member, url).andExpect(status().isOk());
        patch(member, url, Map.of("name", "Renamed")).andExpect(status().isForbidden());
        delete(member, url).andExpect(status().isForbidden());
    }

    @Test
    void update_isPartial_checksDatesAfterTheChange_andCanRestoreAnArchivedProject()
            throws Exception {
        TestUser owner = newUser();
        long workspaceId = createWorkspace(owner);
        long projectId = idOf(post(
                        owner,
                        "/api/workspaces/" + workspaceId + "/projects",
                        Map.of(
                                "name", "Website",
                                "description", "Keep me",
                                "startDate", "2026-10-20",
                                "dueDate", "2026-12-15"))
                .andExpect(status().isCreated()));
        String url = "/api/projects/" + projectId;

        patch(owner, url, Map.of("name", "Website v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Website v2"))
                .andExpect(jsonPath("$.description").value("Keep me"));

        patch(owner, url, Map.of("dueDate", "2020-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Due date cannot be before start date"));

        get(owner, url)
                .andExpect(jsonPath("$.dueDate").value("2026-12-15"));

        patch(owner, url, Map.of("description", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist());

        patch(owner, url, Map.of("status", "ARCHIVED"))
                .andExpect(status().isOk());

        patch(owner, url, Map.of("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void progress_isCalculatedFromTheTasks() throws Exception {
        TestUser owner = newUser();
        long workspaceId = createWorkspace(owner);
        long projectId = createProject(owner, workspaceId);

        long first = createTask(owner, projectId, Map.of("title", "One"));
        long second = createTask(owner, projectId, Map.of("title", "Two"));
        createTask(owner, projectId, Map.of("title", "Three"));
        createTask(owner, projectId, Map.of("title", "Four"));

        patch(owner, "/api/tasks/" + first + "/move", Map.of("status", "IN_REVIEW"))
                .andExpect(status().isOk());
        patch(owner, "/api/tasks/" + first + "/move", Map.of("status", "DONE"))
                .andExpect(status().isOk());

        get(owner, "/api/projects/" + projectId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(4))
                .andExpect(jsonPath("$.completedTasks").value(1))
                .andExpect(jsonPath("$.progressPercent").value(25));

        patch(owner, "/api/tasks/" + second + "/move", Map.of("status", "IN_REVIEW"))
                .andExpect(status().isOk());
        patch(owner, "/api/tasks/" + second + "/move", Map.of("status", "DONE"))
                .andExpect(status().isOk());

        get(owner, "/api/workspaces/" + workspaceId + "/projects")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].completedTasks").value(2))
                .andExpect(jsonPath("$[0].progressPercent").value(50));
    }

    @Test
    void delete_removesTheProjectAndItsTasks() throws Exception {
        TestUser owner = newUser();
        long workspaceId = createWorkspace(owner);
        long projectId = createProject(owner, workspaceId);
        long taskId = createTask(owner, projectId, Map.of("title", "Doomed"));

        delete(owner, "/api/projects/" + projectId)
                .andExpect(status().isNoContent());

        get(owner, "/api/projects/" + projectId)
                .andExpect(status().isNotFound());

        get(owner, "/api/tasks/" + taskId)
                .andExpect(status().isNotFound());

        Integer remaining = jdbcTemplate.queryForObject(
                "select count(*) from tasks where project_id = ?",
                Integer.class,
                projectId);
        assertEquals(0, remaining.intValue());
    }
}