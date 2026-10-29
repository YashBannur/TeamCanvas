package com.teamcanvas.task.dto;

import com.teamcanvas.task.TaskPriority;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Partial update: a field that is missing (null) is left unchanged.
 *  - An empty description ("") clears it.
 *  - "unassign": true removes the assignee ("assigneeId" sets a new one; send only one of them).
 *  - "version" (optional): the version the client last saw; a mismatch gives 409.
 *  - The status cannot be changed here; use PATCH /api/tasks/{id}/move.
 *  - Dates cannot be cleared in the MVP.
 */
public record UpdateTaskRequest(
        @Size(max = 200, message = "Title must be at most 200 characters")
        @Pattern(regexp = ".*\\S.*", message = "Title must not be blank")
        String title,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        TaskPriority priority,

        Long assigneeId,

        Boolean unassign,

        LocalDate startDate,

        LocalDate dueDate,

        Long version) {
}
