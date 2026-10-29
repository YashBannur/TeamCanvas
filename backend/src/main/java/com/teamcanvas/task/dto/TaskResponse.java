package com.teamcanvas.task.dto;

import com.teamcanvas.task.Task;
import com.teamcanvas.task.TaskPriority;
import com.teamcanvas.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Long assigneeId,
        String assigneeName,
        Long reporterId,
        String reporterName,
        LocalDate startDate,
        LocalDate dueDate,
        int position,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getAssignee() == null ? null : task.getAssignee().getId(),
                task.getAssignee() == null ? null : task.getAssignee().getName(),
                task.getReporter().getId(),
                task.getReporter().getName(),
                task.getStartDate(),
                task.getDueDate(),
                task.getSortOrder(),
                task.getVersion(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
