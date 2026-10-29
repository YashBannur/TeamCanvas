package com.teamcanvas.project.dto;

import com.teamcanvas.task.TaskPriority;
import com.teamcanvas.task.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ProjectAnalyticsResponse(
        Long projectId,
        LocalDate from,
        LocalDate to,
        Map<TaskStatus, Long> tasksByStatus,
        Map<TaskPriority, Long> tasksByPriority,
        long completedTasks,
        long pendingTasks,
        long overdueTasks,
        List<AssigneeWorkload> workloadByAssignee,
        List<CompletionTrendPoint> completionTrend) {
}