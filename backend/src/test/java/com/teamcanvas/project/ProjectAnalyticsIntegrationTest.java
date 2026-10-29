package com.teamcanvas.project;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamcanvas.support.ApiTestSupport;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjectAnalyticsIntegrationTest extends ApiTestSupport {

    @Test
    void reportsStatusPriorityWorkloadAndDailyCompletionTrend() throws Exception {
        TestUser owner = newUser();
        long workspaceId = createWorkspace(owner);
        long projectId = createProject(owner, workspaceId);

        long taskId = createTask(
                owner,
                projectId,
                Map.of("title", "Analytics task", "priority", "HIGH"));

        LocalDate today = LocalDate.now();

        patch(owner, "/api/tasks/" + taskId + "/move", Map.of("status", "IN_REVIEW"))
                .andExpect(status().isOk());

        patch(owner, "/api/tasks/" + taskId + "/move", Map.of("status", "DONE"))
                .andExpect(status().isOk());

        get(owner, "/api/projects/" + projectId + "/analytics?from=" + today + "&to=" + today)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasksByStatus.DONE").value(1))
                .andExpect(jsonPath("$.tasksByPriority.HIGH").value(1))
                .andExpect(jsonPath("$.completedTasks").value(1))
                .andExpect(jsonPath("$.pendingTasks").value(0))
                .andExpect(jsonPath("$.overdueTasks").value(0))
                .andExpect(jsonPath("$.workloadByAssignee.length()").value(1))
                .andExpect(jsonPath("$.completionTrend.length()").value(1))
                .andExpect(jsonPath("$.completionTrend[0].completedTasks").value(1));
    }

    @Test
    void returnsZeroFilledTrendForAnEmptyProject() throws Exception {
        TestUser owner = newUser();
        long workspaceId = createWorkspace(owner);
        long projectId = createProject(owner, workspaceId);

        LocalDate from = LocalDate.now().minusDays(2);
        LocalDate to = LocalDate.now();

        get(owner, "/api/projects/" + projectId + "/analytics?from=" + from + "&to=" + to)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedTasks").value(0))
                .andExpect(jsonPath("$.pendingTasks").value(0))
                .andExpect(jsonPath("$.completionTrend.length()").value(3))
                .andExpect(jsonPath("$.completionTrend[0].completedTasks").value(0))
                .andExpect(jsonPath("$.completionTrend[1].completedTasks").value(0))
                .andExpect(jsonPath("$.completionTrend[2].completedTasks").value(0));
    }

    @Test
    void rejectsReversedDateRangeAndHidesProjectFromNonMembers() throws Exception {
        TestUser owner = newUser();
        TestUser outsider = newUser();
        long workspaceId = createWorkspace(owner);
        long projectId = createProject(owner, workspaceId);

        LocalDate today = LocalDate.now();

        get(owner, "/api/projects/" + projectId + "/analytics?from="
                        + today + "&to=" + today.minusDays(1))
                .andExpect(status().isBadRequest());

        get(outsider, "/api/projects/" + projectId + "/analytics")
                .andExpect(status().isNotFound());
    }
}