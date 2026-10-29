package com.teamcanvas.project;

import com.teamcanvas.activity.ActivityLogWriter;
import com.teamcanvas.common.exception.BadRequestException;
import com.teamcanvas.common.exception.ResourceNotFoundException;
import com.teamcanvas.notification.NotificationService;
import com.teamcanvas.project.dto.CreateProjectRequest;
import com.teamcanvas.project.dto.ProjectResponse;
import com.teamcanvas.project.dto.UpdateProjectRequest;
import com.teamcanvas.security.PermissionService;
import com.teamcanvas.task.TaskRepository;
import com.teamcanvas.task.TaskStatus;
import com.teamcanvas.workspace.WorkspaceMember;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private static final String PROJECT_NOT_FOUND = "Project not found";

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final PermissionService permissionService;
    private final ActivityLogWriter activityLogWriter;
    private final NotificationService notificationService;

    public ProjectService(ProjectRepository projectRepository,
                          TaskRepository taskRepository,
                          PermissionService permissionService,
                          ActivityLogWriter activityLogWriter,
                          NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.permissionService = permissionService;
        this.activityLogWriter = activityLogWriter;
        this.notificationService = notificationService;
    }

    @Transactional
    public ProjectResponse create(Long workspaceId, Long actorId, CreateProjectRequest request) {
        WorkspaceMember actor = permissionService.requireAdminOrOwner(workspaceId, actorId);
        validateDates(request.startDate(), request.dueDate());

        Project project = projectRepository.save(new Project(
                actor.getWorkspace(),
                request.name().trim(),
                blankToNull(request.description()),
                request.startDate(),
                request.dueDate(),
                actor.getUser()));

        activityLogWriter.record(
                workspaceId, project.getId(), actorId,
                ActivityLogWriter.PROJECT_CREATED, "PROJECT", project.getId(),
                "name=" + project.getName());

        return ProjectResponse.from(project, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listForWorkspace(Long workspaceId, Long userId, ProjectStatus status) {
        permissionService.requireMember(workspaceId, userId);
        ProjectStatus wanted = status == null ? ProjectStatus.ACTIVE : status;
        List<Project> projects = projectRepository.findByWorkspaceAndStatus(workspaceId, wanted);

        Map<Long, TaskRepository.ProjectTaskCounts> counts =
                countsFor(projects.stream().map(Project::getId).toList());
        return projects.stream().map(project -> toResponse(project, counts)).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getOne(Long projectId, Long userId) {
        Project project = loadAsMember(projectId, userId);
        return toResponse(project, countsFor(List.of(project.getId())));
    }

    @Transactional
    public ProjectResponse update(Long projectId, Long actorId, UpdateProjectRequest request) {
        Project project = loadAsAdminOrOwner(projectId, actorId);
        ProjectStatus previousStatus = project.getStatus();

        if (request.name() != null) {
            project.setName(request.name().trim());
        }
        if (request.description() != null) {
            project.setDescription(blankToNull(request.description()));
        }
        if (request.startDate() != null) {
            project.setStartDate(request.startDate());
        }
        if (request.dueDate() != null) {
            project.setDueDate(request.dueDate());
        }

        validateDates(project.getStartDate(), project.getDueDate());

        if (request.status() != null) {
            project.setStatus(request.status());
        }

        if (previousStatus != ProjectStatus.ARCHIVED
                && project.getStatus() == ProjectStatus.ARCHIVED) {
            activityLogWriter.record(
                    project.getWorkspace().getId(), project.getId(), actorId,
                    ActivityLogWriter.PROJECT_ARCHIVED, "PROJECT", project.getId(), null);

            projectRepository.flush();
            notificationService.projectArchived(project, actorId);
        }

        return toResponse(project, countsFor(List.of(project.getId())));
    }

    @Transactional
    public void delete(Long projectId, Long actorId) {
        Project project = loadAsAdminOrOwner(projectId, actorId);

        activityLogWriter.record(
                project.getWorkspace().getId(), project.getId(), actorId,
                ActivityLogWriter.PROJECT_DELETED, "PROJECT", project.getId(), null);

        projectRepository.delete(project);
    }

    private Project loadAsMember(Long projectId, Long userId) {
        Project project = findOrThrow(projectId);
        permissionService.requireMember(project.getWorkspace().getId(), userId, PROJECT_NOT_FOUND);
        return project;
    }

    private Project loadAsAdminOrOwner(Long projectId, Long userId) {
        Project project = findOrThrow(projectId);
        permissionService.requireAdminOrOwner(project.getWorkspace().getId(), userId, PROJECT_NOT_FOUND);
        return project;
    }

    private Project findOrThrow(Long projectId) {
        return projectRepository.findDetailedById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(PROJECT_NOT_FOUND));
    }

    private Map<Long, TaskRepository.ProjectTaskCounts> countsFor(Collection<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        return taskRepository.countByProjectIds(projectIds, TaskStatus.DONE).stream()
                .collect(Collectors.toMap(TaskRepository.ProjectTaskCounts::getProjectId, Function.identity()));
    }

    private ProjectResponse toResponse(Project project, Map<Long, TaskRepository.ProjectTaskCounts> counts) {
        TaskRepository.ProjectTaskCounts count = counts.get(project.getId());
        if (count == null) {
            return ProjectResponse.from(project, 0, 0);
        }
        return ProjectResponse.from(project, count.getTotal(), count.getCompleted());
    }

    private void validateDates(LocalDate start, LocalDate due) {
        if (start != null && due != null && due.isBefore(start)) {
            throw new BadRequestException("Due date cannot be before start date");
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}