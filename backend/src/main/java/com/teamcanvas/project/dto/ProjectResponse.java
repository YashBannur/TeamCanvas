package com.teamcanvas.project.dto;

import com.teamcanvas.project.Project;
import com.teamcanvas.project.ProjectStatus;
import java.time.Instant;
import java.time.LocalDate;

/** Progress is calculated from the project's tasks (never stored): completed / total, rounded to a percent. */
public record ProjectResponse(
        Long id,
        Long workspaceId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate dueDate,
        ProjectStatus status,
        String createdByName,
        long totalTasks,
        long completedTasks,
        int progressPercent,
        Instant createdAt,
        Instant updatedAt) {

    public static ProjectResponse from(Project project, long totalTasks, long completedTasks) {
        int percent = totalTasks == 0 ? 0 : (int) Math.round(completedTasks * 100.0 / totalTasks);
        return new ProjectResponse(
                project.getId(),
                project.getWorkspace().getId(),
                project.getName(),
                project.getDescription(),
                project.getStartDate(),
                project.getDueDate(),
                project.getStatus(),
                project.getCreatedBy().getName(),
                totalTasks,
                completedTasks,
                percent,
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
