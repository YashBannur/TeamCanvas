package com.teamcanvas.task.dto;

import com.teamcanvas.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Put the task into the column "status", directly below the task "afterTaskId".
 * afterTaskId = null means "at the top of the column".
 */
public record MoveTaskRequest(
        @NotNull(message = "Status is required")
        TaskStatus status,

        Long afterTaskId,

        Long version) {
}
