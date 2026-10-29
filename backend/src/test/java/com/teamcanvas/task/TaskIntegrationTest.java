package com.teamcanvas.task;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamcanvas.support.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Task rules: defaults, ordering, permissions, assignment, versions, moves, archived projects. */
class TaskIntegrationTest extends ApiTestSupport {

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

    private String tasksUrl(Team team) {
        return "/api/projects/" + team.projectId() + "/tasks";
    }

    private String task(long id) {
        return "/api/tasks/" + id;
    }

    private String boardUrl(Team team) {
        return "/api/projects/" + team.projectId() + "/board";
    }

    // ---------------------------------------------------------------- create and view

    @Test
    void create_setsDefaults_andAppendsToTheBottomOfItsColumn() throws Exception {
        Team team = newTeam();

        post(team.owner(), tasksUrl(team), Map.of("title", "First"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.position").value(0))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.reporterId").value(team.owner().id()))
                .andExpect(jsonPath("$.assigneeId").doesNotExist());

        post(team.owner(), tasksUrl(team), Map.of("title", "Second"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(1));

        post(team.owner(), tasksUrl(team), Map.of("title", "Doing", "status", "IN_PROGRESS"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(0));
    }

    @Test
    void create_rejectsInvalidInput_andNonMemberAssignees() throws Exception {
        Team team = newTeam();
        TestUser outsider = newUser();

        post(team.owner(), tasksUrl(team), Map.of("title", "  "))
                .andExpect(status().isBadRequest());

        post(team.owner(), tasksUrl(team), Map.of("title", "X", "priority", "EXTREME"))
                .andExpect(status().isBadRequest());

        post(
                        team.owner(),
                        tasksUrl(team),
                        Map.of("title", "X", "startDate", "2026-12-01", "dueDate", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Due date cannot be before start date"));

        post(team.owner(), tasksUrl(team), Map.of("title", "X", "assigneeId", outsider.id()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Assignee must be a member of this workspace"));
    }

    @Test
    void create_isOpenToMembers_butNotToOutsiders() throws Exception {
        Team team = newTeam();
        TestUser outsider = newUser();

        post(team.member(), tasksUrl(team), Map.of("title", "From a member"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reporterId").value(team.member().id()));

        post(outsider, tasksUrl(team), Map.of("title", "Sneaky"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void view_isOpenToMembers_andOutsidersGetTheSame404AsForAMissingTask() throws Exception {
        Team team = newTeam();
        TestUser outsider = newUser();
        long taskId = createTask(team.owner(), team.projectId(), Map.of("title", "Visible"));

        get(team.member(), task(taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Visible"));

        get(outsider, task(taskId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));

        get(outsider, task(999999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    // ---------------------------------------------------------------- task detail permissions

    @Test
    void memberCannotEditOrReassignTaskCreatedBySomeoneElse() throws Exception {
        Team team = newTeam();
        long taskId = createTask(
                team.owner(),
                team.projectId(),
                Map.of("title", "Assigned to member", "assigneeId", team.member().id()));

        patch(team.member(), task(taskId), Map.of("title", "Changed by assignee"))
                .andExpect(status().isForbidden());

        patch(team.member(), task(taskId), Map.of("assigneeId", team.admin().id()))
                .andExpect(status().isForbidden());

        patch(team.member(), task(taskId), Map.of("unassign", true))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanEditOwnCreatedTask_butCannotEditOtherUsersTasks() throws Exception {
        Team team = newTeam();

        long unassignedTask =
                createTask(team.owner(), team.projectId(), Map.of("title", "Owner's unassigned task"));

        long assignedToMember = createTask(
                team.owner(),
                team.projectId(),
                Map.of("title", "Owner's task assigned to member", "assigneeId", team.member().id()));

        long createdByMember = createTask(
                team.member(),
                team.projectId(),
                Map.of("title", "Member's own task", "assigneeId", team.admin().id()));

        patch(team.member(), task(unassignedTask), Map.of("priority", "HIGH"))
                .andExpect(status().isForbidden());

        patch(team.member(), task(assignedToMember), Map.of("priority", "HIGH"))
                .andExpect(status().isForbidden());

        patch(team.member(), task(createdByMember), Map.of("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"));

        patch(team.member(), task(createdByMember), Map.of("assigneeId", team.owner().id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(team.owner().id()));
    }

    @Test
    void admin_canEditAndDeleteAnyTask() throws Exception {
        Team team = newTeam();
        long taskId = createTask(
                team.owner(),
                team.projectId(),
                Map.of("title", "Member's work", "assigneeId", team.member().id()));

        patch(team.admin(), task(taskId), Map.of("priority", "URGENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("URGENT"));

        patch(team.admin(), task(taskId), Map.of("assigneeId", team.admin().id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(team.admin().id()));

        patch(team.admin(), task(taskId) + "/move", Map.of("status", "IN_REVIEW"))
                .andExpect(status().isOk());

        delete(team.admin(), task(taskId)).andExpect(status().isNoContent());
        get(team.admin(), task(taskId)).andExpect(status().isNotFound());
    }

    @Test
    void delete_memberCanDeleteOnlyOwnCreatedTasks() throws Exception {
        Team team = newTeam();
        long own = createTask(team.member(), team.projectId(), Map.of("title", "Mine"));
        long others = createTask(team.owner(), team.projectId(), Map.of("title", "The owner's"));

        delete(team.member(), task(others))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only delete tasks you created"));

        delete(team.member(), task(own)).andExpect(status().isNoContent());
        get(team.member(), task(own)).andExpect(status().isNotFound());

        delete(team.admin(), task(others)).andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------- assignment and updates

    @Test
    void creatorCanAssignAndUnassignTask() throws Exception {
        Team team = newTeam();
        TestUser outsider = newUser();
        long taskId = createTask(team.owner(), team.projectId(), Map.of("title", "Assign me"));

        patch(team.owner(), task(taskId), Map.of("assigneeId", team.member().id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(team.member().id()));

        patch(team.owner(), task(taskId), Map.of("assigneeId", outsider.id()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Assignee must be a member of this workspace"));

        patch(
                        team.owner(),
                        task(taskId),
                        Map.of("assigneeId", team.member().id(), "unassign", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Send either assigneeId or unassign, not both"));

        patch(team.owner(), task(taskId), Map.of("unassign", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").doesNotExist());
    }

    @Test
    void update_isPartial_andChecksDates() throws Exception {
        Team team = newTeam();
        long taskId = createTask(
                team.owner(),
                team.projectId(),
                Map.of(
                        "title", "Original",
                        "priority", "HIGH",
                        "startDate", "2026-10-20",
                        "dueDate", "2026-12-15"));

        patch(team.owner(), task(taskId), Map.of("title", "Renamed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        patch(team.owner(), task(taskId), Map.of("dueDate", "2020-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Due date cannot be before start date"));

        get(team.owner(), task(taskId))
                .andExpect(jsonPath("$.dueDate").value("2026-12-15"));

        patch(team.owner(), task(taskId), Map.of("title", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void optimisticLocking_staleVersionReturns409() throws Exception {
        Team team = newTeam();
        long taskId = createTask(team.owner(), team.projectId(), Map.of("title", "Contested"));

        patch(team.owner(), task(taskId), Map.of("priority", "URGENT", "version", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        patch(team.owner(), task(taskId), Map.of("priority", "LOW", "version", 0))
                .andExpect(status().isConflict());

        patch(team.owner(), task(taskId), Map.of("priority", "LOW", "version", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    // ---------------------------------------------------------------- moves

    @Test
    void memberCanMoveAssignedTaskButCannotMoveItDirectlyToDone() throws Exception {
        Team team = newTeam();
        long taskId = createTask(
                team.owner(),
                team.projectId(),
                Map.of("title", "Member's task", "assigneeId", team.member().id()));

        patch(team.member(), task(taskId) + "/move", Map.of("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        patch(team.member(), task(taskId) + "/move", Map.of("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("A task must move to In Review before it can be marked Done"));
    }

    @Test
    void memberCannotApproveTaskFromInReview_butAdminCan() throws Exception {
        Team team = newTeam();
        long taskId = createTask(
                team.owner(),
                team.projectId(),
                Map.of("title", "Needs approval", "assigneeId", team.member().id()));

        patch(team.member(), task(taskId) + "/move", Map.of("status", "IN_PROGRESS"))
                .andExpect(status().isOk());

        patch(team.member(), task(taskId) + "/move", Map.of("status", "IN_REVIEW"))
                .andExpect(status().isOk());

        patch(team.member(), task(taskId) + "/move", Map.of("status", "DONE"))
                .andExpect(status().isForbidden());

        patch(team.admin(), task(taskId) + "/move", Map.of("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void move_reordersTheColumn() throws Exception {
        Team team = newTeam();
        long a = createTask(team.owner(), team.projectId(), Map.of("title", "A"));
        long b = createTask(team.owner(), team.projectId(), Map.of("title", "B"));
        long c = createTask(team.owner(), team.projectId(), Map.of("title", "C"));

        patch(team.owner(), task(c) + "/move", Map.of("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(0));

        get(team.owner(), boardUrl(team))
                .andExpect(jsonPath("$.columns.TODO[0].id").value(c))
                .andExpect(jsonPath("$.columns.TODO[1].id").value(a))
                .andExpect(jsonPath("$.columns.TODO[2].id").value(b));

        patch(team.owner(), task(a) + "/move", Map.of("status", "TODO", "afterTaskId", b))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(2));

        get(team.owner(), boardUrl(team))
                .andExpect(jsonPath("$.columns.TODO[0].id").value(c))
                .andExpect(jsonPath("$.columns.TODO[1].id").value(b))
                .andExpect(jsonPath("$.columns.TODO[2].id").value(a));
    }

    @Test
    void move_intoAnotherColumn_afterATask_andRejectsAForeignAfterTask() throws Exception {
        Team team = newTeam();
        long a = createTask(team.owner(), team.projectId(), Map.of("title", "A"));
        long x = createTask(
                team.owner(),
                team.projectId(),
                Map.of("title", "X", "status", "IN_PROGRESS"));
        long y = createTask(
                team.owner(),
                team.projectId(),
                Map.of("title", "Y", "status", "IN_PROGRESS"));

        patch(team.owner(), task(a) + "/move", Map.of("status", "IN_PROGRESS", "afterTaskId", x))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.position").value(1));

        get(team.owner(), boardUrl(team))
                .andExpect(jsonPath("$.columns.IN_PROGRESS[0].id").value(x))
                .andExpect(jsonPath("$.columns.IN_PROGRESS[1].id").value(a))
                .andExpect(jsonPath("$.columns.IN_PROGRESS[2].id").value(y))
                .andExpect(jsonPath("$.columns.TODO.length()").value(0));

        long reviewTask =
                createTask(team.owner(), team.projectId(), Map.of("title", "Review", "status", "IN_REVIEW"));

        patch(
                        team.owner(),
                        task(reviewTask) + "/move",
                        Map.of("status", "DONE", "afterTaskId", x))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("afterTaskId must be a different task in the target column"));

        patch(
                        team.owner(),
                        task(a) + "/move",
                        Map.of("status", "IN_PROGRESS", "afterTaskId", a))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- archived projects

    @Test
    void archivedProject_makesTasksReadOnly() throws Exception {
        Team team = newTeam();
        long taskId = createTask(team.owner(), team.projectId(), Map.of("title", "Frozen"));
        String projectUrl = "/api/projects/" + team.projectId();

        patch(team.owner(), projectUrl, Map.of("status", "ARCHIVED"))
                .andExpect(status().isOk());

        post(team.owner(), tasksUrl(team), Map.of("title", "Late"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This project is archived; restore it to change its tasks"));

        patch(team.owner(), task(taskId), Map.of("priority", "LOW"))
                .andExpect(status().isConflict());

        patch(team.owner(), task(taskId) + "/move", Map.of("status", "DONE"))
                .andExpect(status().isConflict());

        delete(team.owner(), task(taskId)).andExpect(status().isConflict());
        get(team.owner(), task(taskId)).andExpect(status().isOk());

        patch(team.owner(), projectUrl, Map.of("status", "ACTIVE"))
                .andExpect(status().isOk());

        patch(team.owner(), task(taskId), Map.of("priority", "LOW"))
                .andExpect(status().isOk());
    }
}