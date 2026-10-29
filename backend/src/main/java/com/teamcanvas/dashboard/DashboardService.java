package com.teamcanvas.dashboard;

import com.teamcanvas.dashboard.dto.DashboardResponse;
import com.teamcanvas.dashboard.dto.TaskSummaryResponse;
import com.teamcanvas.project.Project;
import com.teamcanvas.project.dto.ProjectResponse;
import com.teamcanvas.task.TaskRepository;
import com.teamcanvas.task.TaskStatus;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final int DUE_SOON_DAYS = 7;
    private static final int MY_TASKS_LIMIT = 8;
    private static final int RECENT_PROJECTS_LIMIT = 5;
    private static final int RECENT_TASKS_LIMIT = 8;

    private final DashboardRepository dashboardRepository;
    private final TaskRepository taskRepository;

    public DashboardService(DashboardRepository dashboardRepository, TaskRepository taskRepository) {
        this.dashboardRepository = dashboardRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * @param today the caller's local date (so "overdue" and "due soon" are right in their time zone);
     *              falls back to today's date in UTC when the client does not send it.
     */
    @Transactional(readOnly = true)
    public DashboardResponse get(Long userId, LocalDate today) {
        LocalDate date = today == null ? LocalDate.now(ZoneOffset.UTC) : today;

        List<Long> workspaceIds = dashboardRepository.findWorkspaceIds(userId);
        if (workspaceIds.isEmpty()) {
            return DashboardResponse.empty();
        }

        DashboardRepository.TaskCounts taskCounts = dashboardRepository.countMyOpenTasks(
                userId, workspaceIds, date, date.plusDays(DUE_SOON_DAYS));
        long activeProjects = dashboardRepository.countActiveProjects(workspaceIds);

        List<TaskSummaryResponse> myTasks = dashboardRepository
                .findMyOpenTasks(userId, workspaceIds, MY_TASKS_LIMIT).stream()
                .map(TaskSummaryResponse::from)
                .toList();
        List<TaskSummaryResponse> recentTasks = dashboardRepository
                .findRecentTasks(workspaceIds, RECENT_TASKS_LIMIT).stream()
                .map(TaskSummaryResponse::from)
                .toList();

        List<Project> projects = dashboardRepository.findRecentActiveProjects(workspaceIds, RECENT_PROJECTS_LIMIT);
        Map<Long, TaskRepository.ProjectTaskCounts> progress = progressFor(projects);
        List<ProjectResponse> recentProjects = projects.stream()
                .map(project -> toResponse(project, progress))
                .toList();

        return new DashboardResponse(
                new DashboardResponse.Counts(workspaceIds.size(), activeProjects,
                        taskCounts.open(), taskCounts.dueSoon(), taskCounts.overdue()),
                myTasks,
                recentProjects,
                recentTasks);
    }

    /** Progress of all listed projects in ONE grouped query. */
    private Map<Long, TaskRepository.ProjectTaskCounts> progressFor(List<Project> projects) {
        if (projects.isEmpty()) {
            return Map.of();
        }
        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        return taskRepository.countByProjectIds(projectIds, TaskStatus.DONE).stream()
                .collect(Collectors.toMap(TaskRepository.ProjectTaskCounts::getProjectId, Function.identity()));
    }

    private ProjectResponse toResponse(Project project, Map<Long, TaskRepository.ProjectTaskCounts> progress) {
        TaskRepository.ProjectTaskCounts counts = progress.get(project.getId());
        if (counts == null) {
            return ProjectResponse.from(project, 0, 0);
        }
        return ProjectResponse.from(project, counts.getTotal(), counts.getCompleted());
    }
}