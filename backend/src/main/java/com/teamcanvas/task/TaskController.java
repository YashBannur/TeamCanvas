package com.teamcanvas.task;

import com.teamcanvas.security.UserPrincipal;
import com.teamcanvas.task.dto.CreateTaskRequest;
import com.teamcanvas.task.dto.MoveTaskRequest;
import com.teamcanvas.task.dto.TaskResponse;
import com.teamcanvas.task.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Thin controller: the caller's id always comes from the verified JWT. */
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long projectId,
                               @Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(projectId, principal.getId(), request);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse getOne(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long taskId) {
        return taskService.getOne(taskId, principal.getId());
    }

    @PatchMapping("/tasks/{taskId}")
    public TaskResponse update(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long taskId,
                               @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(taskId, principal.getId(), request);
    }

    @PatchMapping("/tasks/{taskId}/move")
    public TaskResponse move(@AuthenticationPrincipal UserPrincipal principal,
                             @PathVariable Long taskId,
                             @Valid @RequestBody MoveTaskRequest request) {
        return taskService.move(taskId, principal.getId(), request);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long taskId) {
        taskService.delete(taskId, principal.getId());
    }
}
