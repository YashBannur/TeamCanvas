package com.teamcanvas.activity;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamcanvas.support.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaskActivityIntegrationTest extends ApiTestSupport {

    private record Team(
            TestUser owner,
            TestUser admin,
            TestUser member,
            long workspaceId,
            long projectId) {
    }

    private Team newTeam() throws Exception {
        TestUser owner = newUser();
        TestUser admin = newUser();
        TestUser member = newUser();

        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, admin, "ADMIN");
        addMember(owner, workspaceId, member, "MEMBER");
        long projectId = createProject(owner, workspaceId);

        return new Team(owner, admin, member, workspaceId, projectId);
    }

    @Test
    void taskActivityShowsStatusChangesAndWhoApprovedDone() throws Exception {
        Team team = newTeam();
        long taskId = createTask(
                team.owner(),
                team.projectId(),
                Map.of("title", "Build feature"));

        patch(team.member(), "/api/tasks/" + taskId + "/move",
                        Map.of("status", "IN_PROGRESS"))
                .andExpect(status().isOk());

        patch(team.member(), "/api/tasks/" + taskId + "/move",
                        Map.of("status", "IN_REVIEW"))
                .andExpect(status().isOk());

        patch(team.admin(), "/api/tasks/" + taskId + "/move",
                        Map.of("status", "DONE"))
                .andExpect(status().isOk());

        get(team.member(), "/api/tasks/" + taskId + "/activity")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(4))
                .andExpect(jsonPath("$.content[0].action").value("TASK_APPROVED"))
                .andExpect(jsonPath("$.content[0].actorId").value(team.admin().id()))
                .andExpect(jsonPath("$.content[0].details")
                        .value("status: IN_REVIEW -> DONE"))
                .andExpect(jsonPath("$.content[1].action")
                        .value("TASK_STATUS_CHANGED"))
                .andExpect(jsonPath("$.content[1].actorId").value(team.member().id()))
                .andExpect(jsonPath("$.content[1].details")
                        .value("status: IN_PROGRESS -> IN_REVIEW"))
                .andExpect(jsonPath("$.content[2].details")
                        .value("status: TODO -> IN_PROGRESS"))
                .andExpect(jsonPath("$.content[3].action").value("TASK_CREATED"));
    }

    @Test
    void taskActivityShowsCreatorEditsAndAssigneeChanges() throws Exception {
        Team team = newTeam();
        long taskId = createTask(
                team.member(),
                team.projectId(),
                Map.of("title", "Original title"));

        patch(team.member(), "/api/tasks/" + taskId,
                        Map.of(
                                "title", "Updated title",
                                "assigneeId", team.admin().id()))
                .andExpect(status().isOk());

        get(team.member(), "/api/tasks/" + taskId + "/activity")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("TASK_UPDATED"))
                .andExpect(jsonPath("$.content[0].actorId").value(team.member().id()))
                .andExpect(jsonPath("$.content[0].details")
                        .value("title: Original title -> Updated title"))
                .andExpect(jsonPath("$.content[1].action").value("TASK_ASSIGNED"))
                .andExpect(jsonPath("$.content[1].actorId").value(team.member().id()))
                .andExpect(jsonPath("$.content[1].details")
                        .value("assignee: Unassigned -> Test User"))
                .andExpect(jsonPath("$.content[2].action").value("TASK_CREATED"));
    }

    @Test
    void outsiderCannotReadTaskActivity() throws Exception {
        Team team = newTeam();
        TestUser outsider = newUser();
        long taskId = createTask(
                team.owner(),
                team.projectId(),
                Map.of("title", "Private task"));

        get(outsider, "/api/tasks/" + taskId + "/activity")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }
}