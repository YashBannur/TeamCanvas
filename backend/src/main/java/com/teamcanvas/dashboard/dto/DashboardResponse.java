package com.teamcanvas.dashboard.dto;

import com.teamcanvas.project.dto.ProjectResponse;
import java.util.List;

public record DashboardResponse(
        Counts counts,
        List<TaskSummaryResponse> myTasks,
        List<ProjectResponse> recentProjects,
        List<TaskSummaryResponse> recentTasks) {

    /**
     * openTasks: assigned to me, not DONE, in an ACTIVE project.
     * dueSoon:   those with a due date from today to today + 7 (inclusive).
     * overdue:   those with a due date before today.
     */
    public record Counts(long workspaces, long activeProjects, long openTasks, long dueSoon, long overdue) {
    }

    public static DashboardResponse empty() {
        return new DashboardResponse(new Counts(0, 0, 0, 0, 0), List.of(), List.of(), List.of());
    }
}