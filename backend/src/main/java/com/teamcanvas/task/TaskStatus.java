package com.teamcanvas.task;

/** The four Kanban columns. Any column-to-column move is allowed; every move is a normal, validated update. */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE
}
