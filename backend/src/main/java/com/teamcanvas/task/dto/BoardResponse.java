package com.teamcanvas.task.dto;

import com.teamcanvas.task.TaskStatus;
import java.util.List;
import java.util.Map;

/**
 * The Kanban board: all four columns are always present (possibly empty), each ordered top to bottom.
 * "truncated" is true if the project has more tasks than the board returns (500).
 */
public record BoardResponse(Map<TaskStatus, List<TaskResponse>> columns, boolean truncated) {
}
