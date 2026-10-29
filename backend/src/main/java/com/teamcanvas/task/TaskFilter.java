package com.teamcanvas.task;

import java.time.LocalDate;

/** The optional search criteria of the task list and the board. A null field means "do not filter by this". */
public record TaskFilter(
        String q,
        TaskStatus status,
        TaskPriority priority,
        Long assigneeId,
        Boolean unassigned,
        LocalDate dueFrom,
        LocalDate dueTo) {
}
