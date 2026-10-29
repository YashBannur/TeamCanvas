package com.teamcanvas.activity;

import com.teamcanvas.common.dto.PageResponse;
import com.teamcanvas.common.exception.ResourceNotFoundException;
import com.teamcanvas.project.Project;
import com.teamcanvas.project.ProjectRepository;
import com.teamcanvas.security.PermissionService;
import com.teamcanvas.task.Task;
import com.teamcanvas.task.TaskRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityQueryService {

    private static final String PROJECT_NOT_FOUND = "Project not found";
    private static final String TASK_NOT_FOUND = "Task not found";

    private final ActivityLogRepository activityRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final PermissionService permissionService;

    public ActivityQueryService(
            ActivityLogRepository activityRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            PermissionService permissionService) {
        this.activityRepository = activityRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityLogResponse> projectActivity(
            Long projectId, Long userId, int page, int size) {
        Project project = projectRepository.findDetailedById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(PROJECT_NOT_FOUND));

        permissionService.requireMember(
                project.getWorkspace().getId(), userId, PROJECT_NOT_FOUND);

        Page<ActivityLog> result = activityRepository.findByProjectId(
                projectId, pageRequest(page, size));
        return PageResponse.from(result.map(ActivityLogResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityLogResponse> taskActivity(
            Long taskId, Long userId, int page, int size) {
        Task task = taskRepository.findDetailedById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(TASK_NOT_FOUND));

        permissionService.requireMember(
                task.getProject().getWorkspace().getId(), userId, TASK_NOT_FOUND);

        Page<ActivityLog> result =
                activityRepository.findByProjectIdAndTargetTypeAndTargetId(
                        task.getProject().getId(),
                        "TASK",
                        taskId,
                        pageRequest(page, size));

        return PageResponse.from(result.map(ActivityLogResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityLogResponse> workspaceActivity(
            Long workspaceId, Long userId, int page, int size) {
        permissionService.requireMember(workspaceId, userId);

        Page<ActivityLog> result = activityRepository.findByWorkspaceId(
                workspaceId, pageRequest(page, size));
        return PageResponse.from(result.map(ActivityLogResponse::from));
    }

    private PageRequest pageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));

        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id"));

        return PageRequest.of(safePage, safeSize, sort);
    }
}