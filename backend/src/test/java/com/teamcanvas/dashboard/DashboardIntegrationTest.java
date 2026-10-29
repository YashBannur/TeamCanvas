package com.teamcanvas.dashboard;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamcanvas.support.ApiTestSupport;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** The dashboard numbers and lists, and the rule that users only see their own workspaces' data. */
class DashboardIntegrationTest extends ApiTestSupport {

    /** Tests pin the browser's local date so they never depend on the real clock. */
    private static final String DASHBOARD = "/api/dashboard?today=2026-10-20";

    private record Setup(TestUser owner, TestUser member, long workspaceId, long projectId) {
    }

    private long task(
            TestUser creator,
            long projectId,
            String title,
            TestUser assignee,
            String dueDate,
            String status) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        if (assignee != null) {
            body.put("assigneeId", assignee.id());
        }
        if (dueDate != null) {
            body.put("dueDate", dueDate);
        }
        if (status != null) {
            body.put("status", status);
        }
        return createTask(creator, projectId, body);
    }

    /**
     * With today = 2026-10-20, the owner has five open tasks and one completed task.
     * The member has one task due 2026-10-19.
     */
    private Setup setupWithTasks() throws Exception {
        TestUser owner = newUser();
        TestUser member = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, member, "MEMBER");
        long projectId = createProject(owner, workspaceId);

        task(owner, projectId, "overdue", owner, "2026-10-18", null);
        task(owner, projectId, "today", owner, "2026-10-20", null);
        task(owner, projectId, "edge-in", owner, "2026-10-27", null);
        task(owner, projectId, "edge-out", owner, "2026-10-28", null);
        task(owner, projectId, "no-date", owner, null, null);

        long finished = task(owner, projectId, "finished", owner, "2026-10-18", null);
        patch(owner, "/api/tasks/" + finished + "/move", Map.of("status", "IN_REVIEW"))
                .andExpect(status().isOk());
        patch(owner, "/api/tasks/" + finished + "/move", Map.of("status", "DONE"))
                .andExpect(status().isOk());

        task(owner, projectId, "members-task", member, "2026-10-19", null);

        return new Setup(owner, member, workspaceId, projectId);
    }

    @Test
    void newUser_getsZerosAndEmptyLists() throws Exception {
        TestUser user = newUser();

        get(user, DASHBOARD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.workspaces").value(0))
                .andExpect(jsonPath("$.counts.activeProjects").value(0))
                .andExpect(jsonPath("$.counts.openTasks").value(0))
                .andExpect(jsonPath("$.counts.dueSoon").value(0))
                .andExpect(jsonPath("$.counts.overdue").value(0))
                .andExpect(jsonPath("$.myTasks.length()").value(0))
                .andExpect(jsonPath("$.recentProjects.length()").value(0))
                .andExpect(jsonPath("$.recentTasks.length()").value(0));
    }

    @Test
    void counts_reflectOnlyTheCallersOpenTasks() throws Exception {
        Setup setup = setupWithTasks();

        get(setup.owner(), DASHBOARD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.workspaces").value(1))
                .andExpect(jsonPath("$.counts.activeProjects").value(1))
                .andExpect(jsonPath("$.counts.openTasks").value(5))
                .andExpect(jsonPath("$.counts.dueSoon").value(2))
                .andExpect(jsonPath("$.counts.overdue").value(1));

        get(setup.member(), DASHBOARD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.workspaces").value(1))
                .andExpect(jsonPath("$.counts.openTasks").value(1))
                .andExpect(jsonPath("$.counts.dueSoon").value(0))
                .andExpect(jsonPath("$.counts.overdue").value(1));
    }

    @Test
    void myTasks_areSortedByDueDate_noDateLast_andExcludeDoneTasks() throws Exception {
        Setup setup = setupWithTasks();

        get(setup.owner(), DASHBOARD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myTasks.length()").value(5))
                .andExpect(jsonPath("$.myTasks[0].title").value("overdue"))
                .andExpect(jsonPath("$.myTasks[1].title").value("today"))
                .andExpect(jsonPath("$.myTasks[2].title").value("edge-in"))
                .andExpect(jsonPath("$.myTasks[3].title").value("edge-out"))
                .andExpect(jsonPath("$.myTasks[4].title").value("no-date"))
                .andExpect(jsonPath("$.myTasks[0].projectName").isNotEmpty())
                .andExpect(jsonPath("$.myTasks[0].workspaceName").isNotEmpty())
                .andExpect(jsonPath("$.myTasks[0].workspaceId").value(setup.workspaceId()))
                .andExpect(jsonPath("$.myTasks[0].assigneeName").isNotEmpty());
    }

    @Test
    void archivedProjects_areIgnoredForOpenTasksAndActiveProjects() throws Exception {
        TestUser owner = newUser();
        long workspaceId = createWorkspace(owner);
        long projectId = createProject(owner, workspaceId);
        task(owner, projectId, "mine", owner, "2026-10-21", null);

        patch(owner, "/api/projects/" + projectId, Map.of("status", "ARCHIVED"))
                .andExpect(status().isOk());

        get(owner, DASHBOARD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.workspaces").value(1))
                .andExpect(jsonPath("$.counts.activeProjects").value(0))
                .andExpect(jsonPath("$.counts.openTasks").value(0))
                .andExpect(jsonPath("$.counts.dueSoon").value(0))
                .andExpect(jsonPath("$.myTasks.length()").value(0))
                .andExpect(jsonPath("$.recentProjects.length()").value(0));
    }

    @Test
    void recentProjects_areLimitedToFive_newestFirst_withProgress() throws Exception {
        TestUser owner = newUser();
        long workspaceId = createWorkspace(owner);
        long newest = 0;

        for (int i = 0; i < 6; i++) {
            newest = createProject(owner, workspaceId);
        }

        long finished = task(owner, newest, "finished", null, null, null);
        task(owner, newest, "open", null, null, null);

        patch(owner, "/api/tasks/" + finished + "/move", Map.of("status", "IN_REVIEW"))
                .andExpect(status().isOk());
        patch(owner, "/api/tasks/" + finished + "/move", Map.of("status", "DONE"))
                .andExpect(status().isOk());

        get(owner, DASHBOARD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.activeProjects").value(6))
                .andExpect(jsonPath("$.recentProjects.length()").value(5))
                .andExpect(jsonPath("$.recentProjects[0].id").value(newest))
                .andExpect(jsonPath("$.recentProjects[0].totalTasks").value(2))
                .andExpect(jsonPath("$.recentProjects[0].completedTasks").value(1))
                .andExpect(jsonPath("$.recentProjects[0].progressPercent").value(50));
    }

    @Test
    void recentTasks_areLimitedToEight_mostRecentlyUpdatedFirst() throws Exception {
        TestUser owner = newUser();
        long workspaceId = createWorkspace(owner);
        long projectId = createProject(owner, workspaceId);

        for (int i = 1; i <= 9; i++) {
            task(owner, projectId, "task-" + i, null, null, null);
        }

        get(owner, DASHBOARD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentTasks.length()").value(8))
                .andExpect(jsonPath("$.recentTasks[0].title").value("task-9"))
                .andExpect(jsonPath("$.recentTasks[7].title").value("task-2"))
                .andExpect(jsonPath("$.myTasks.length()").value(0));
    }

    /** Another user's workspaces must not appear until the caller joins them. */
    @Test
    void dashboard_onlyShowsWorkspacesTheCallerBelongsTo() throws Exception {
        TestUser owner = newUser();
        TestUser stranger = newUser();

        long ownWorkspace = createWorkspace(owner);
        long ownProject = createProject(owner, ownWorkspace);
        task(owner, ownProject, "my-task", owner, "2026-10-21", null);

        long otherWorkspace = createWorkspace(stranger);
        long otherProject = createProject(stranger, otherWorkspace);
        task(stranger, otherProject, "stranger-task", stranger, "2026-10-19", null);

        get(owner, DASHBOARD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.workspaces").value(1))
                .andExpect(jsonPath("$.counts.activeProjects").value(1))
                .andExpect(jsonPath("$.counts.openTasks").value(1))
                .andExpect(jsonPath("$.counts.overdue").value(0))
                .andExpect(jsonPath("$.recentProjects.length()").value(1))
                .andExpect(jsonPath("$.recentTasks[?(@.title == 'stranger-task')]").isEmpty());

        addMember(stranger, otherWorkspace, owner, "MEMBER");

        get(owner, DASHBOARD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.workspaces").value(2))
                .andExpect(jsonPath("$.counts.activeProjects").value(2))
                .andExpect(jsonPath("$.counts.openTasks").value(1));
    }

    @Test
    void todayParameter_isOptional_butMustBeAValidDate() throws Exception {
        TestUser user = newUser();

        get(user, "/api/dashboard").andExpect(status().isOk());
        get(user, "/api/dashboard?today=not-a-date").andExpect(status().isBadRequest());
    }

    @Test
    void dashboard_requiresAuthentication() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }
}