package com.teamcanvas.dashboard.dto;

import com.teamcanvas.project.Project;
import com.teamcanvas.task.Task;
import com.teamcanvas.task.TaskPriority;
import com.teamcanvas.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

/** A task with just enough context (project and workspace names) to show it in a list on the dashboard. */
public record TaskSummaryResponse(
        Long id,
        Long projectId,
        String projectName,
        Long workspaceId,
        String workspaceName,
        String title,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        String assigneeName,
        Instant updatedAt) {

    public static TaskSummaryResponse from(Task task) {
        Project project = task.getProject();
        return new TaskSummaryResponse(
                task.getId(),
                project.getId(),
                project.getName(),
                project.getWorkspace().getId(),
                project.getWorkspace().getName(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getAssignee() == null ? null : task.getAssignee().getName(),
                task.getUpdatedAt());
    }
}