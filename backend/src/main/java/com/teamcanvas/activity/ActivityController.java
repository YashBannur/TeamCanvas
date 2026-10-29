package com.teamcanvas.activity;

import com.teamcanvas.common.dto.PageResponse;
import com.teamcanvas.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ActivityController {

    private final ActivityQueryService activityQueryService;

    public ActivityController(ActivityQueryService activityQueryService) {
        this.activityQueryService = activityQueryService;
    }

    @GetMapping("/projects/{projectId}/activity")
    public PageResponse<ActivityLogResponse> projectActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return activityQueryService.projectActivity(
                projectId, principal.getId(), page, size);
    }

    @GetMapping("/tasks/{taskId}/activity")
    public PageResponse<ActivityLogResponse> taskActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return activityQueryService.taskActivity(
                taskId, principal.getId(), page, size);
    }

    @GetMapping("/workspaces/{workspaceId}/activity")
    public PageResponse<ActivityLogResponse> workspaceActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return activityQueryService.workspaceActivity(
                workspaceId, principal.getId(), page, size);
    }
}