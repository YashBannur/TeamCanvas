package com.teamcanvas.comment;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamcanvas.support.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaskCommentIntegrationTest extends ApiTestSupport {

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

    private String commentsUrl(long taskId) {
        return "/api/tasks/" + taskId + "/comments";
    }

    private String commentUrl(long commentId) {
        return "/api/comments/" + commentId;
    }

    @Test
    void memberCanListAndCreateComments() throws Exception {
        Team team = newTeam();
        long taskId = createTask(team.member(), team.projectId(), Map.of("title", "Commented task"));

        long commentId = idOf(post(
                        team.member(),
                        commentsUrl(taskId),
                        Map.of("content", "  First comment  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.authorId").value(team.member().id()))
                .andExpect(jsonPath("$.content").value("First comment")));

        get(team.owner(), commentsUrl(taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(commentId))
                .andExpect(jsonPath("$[0].content").value("First comment"));
    }

    @Test
    void nonMembersReceive404ForTaskAndCommentRoutes() throws Exception {
        Team team = newTeam();
        TestUser outsider = newUser();
        long taskId = createTask(team.owner(), team.projectId(), Map.of("title", "Private task"));

        long commentId = idOf(post(
                        team.member(),
                        commentsUrl(taskId),
                        Map.of("content", "Member comment"))
                .andExpect(status().isCreated()));

        get(outsider, commentsUrl(taskId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));

        post(outsider, commentsUrl(taskId), Map.of("content", "Outsider comment"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));

        patch(outsider, commentUrl(commentId), Map.of("content", "Changed"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Comment not found"));

        delete(outsider, commentUrl(commentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Comment not found"));
    }

    @Test
    void onlyAuthorCanEditAndOwnerOrAdminCanDelete() throws Exception {
        Team team = newTeam();
        TestUser otherMember = newUser();
        addMember(team.owner(), team.workspaceId(), otherMember, "MEMBER");

        long taskId = createTask(team.member(), team.projectId(), Map.of("title", "Commented task"));
        long commentId = idOf(post(
                        team.member(),
                        commentsUrl(taskId),
                        Map.of("content", "Original"))
                .andExpect(status().isCreated()));

        patch(team.member(), commentUrl(commentId), Map.of("content", "Updated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated"));

        patch(otherMember, commentUrl(commentId), Map.of("content", "Not allowed"))
                .andExpect(status().isForbidden());

        delete(otherMember, commentUrl(commentId))
                .andExpect(status().isForbidden());

        delete(team.admin(), commentUrl(commentId))
                .andExpect(status().isNoContent());

        get(team.member(), commentsUrl(taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void archivedProjectBlocksCommentWrites() throws Exception {
        Team team = newTeam();
        long taskId = createTask(team.owner(), team.projectId(), Map.of("title", "Archived task"));
        long commentId = idOf(post(
                        team.owner(),
                        commentsUrl(taskId),
                        Map.of("content", "Existing comment"))
                .andExpect(status().isCreated()));

        patch(team.owner(), "/api/projects/" + team.projectId(), Map.of("status", "ARCHIVED"))
                .andExpect(status().isOk());

        post(team.owner(), commentsUrl(taskId), Map.of("content", "New comment"))
                .andExpect(status().isConflict());

        patch(team.owner(), commentUrl(commentId), Map.of("content", "Changed comment"))
                .andExpect(status().isConflict());

        delete(team.owner(), commentUrl(commentId))
                .andExpect(status().isConflict());
    }

    @Test
    void commentContentMustContainOneToTwoThousandCharacters() throws Exception {
        Team team = newTeam();
        long taskId = createTask(team.owner(), team.projectId(), Map.of("title", "Validated task"));

        post(team.owner(), commentsUrl(taskId), Map.of("content", "   "))
                .andExpect(status().isBadRequest());

        post(team.owner(), commentsUrl(taskId), Map.of("content", "x".repeat(2001)))
                .andExpect(status().isBadRequest());
    }
}