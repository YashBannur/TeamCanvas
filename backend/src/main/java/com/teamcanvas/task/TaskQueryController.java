package com.teamcanvas.task;

import com.teamcanvas.common.dto.PageResponse;
import com.teamcanvas.security.UserPrincipal;
import com.teamcanvas.task.dto.BoardResponse;
import com.teamcanvas.task.dto.TaskResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Thin controller for the read side of tasks. The caller's id always comes from the verified JWT. */
@RestController
@RequestMapping("/api")
public class TaskQueryController {

    private final TaskQueryService taskQueryService;

    public TaskQueryController(TaskQueryService taskQueryService) {
        this.taskQueryService = taskQueryService;
    }

    @GetMapping("/projects/{projectId}/tasks")
    public PageResponse<TaskResponse> search(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long projectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        TaskFilter filter = new TaskFilter(q, status, priority, assigneeId, unassigned, dueFrom, dueTo);
        return taskQueryService.search(projectId, principal.getId(), filter, page, size, sortBy, direction);
    }

    @GetMapping("/projects/{projectId}/board")
    public BoardResponse board(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long projectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo) {
        TaskFilter filter = new TaskFilter(q, status, priority, assigneeId, unassigned, dueFrom, dueTo);
        return taskQueryService.board(projectId, principal.getId(), filter);
    }
}
