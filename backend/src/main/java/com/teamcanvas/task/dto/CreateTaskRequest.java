package com.teamcanvas.task.dto;

import com.teamcanvas.task.TaskPriority;
import com.teamcanvas.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** "status" defaults to TODO and "priority" to MEDIUM. The reporter is always the caller. */
public record CreateTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        TaskStatus status,

        TaskPriority priority,

        Long assigneeId,

        LocalDate startDate,

        LocalDate dueDate) {
}
