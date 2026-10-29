package com.teamcanvas.project;

import com.teamcanvas.common.exception.BadRequestException;
import com.teamcanvas.common.exception.ResourceNotFoundException;
import com.teamcanvas.project.dto.AssigneeWorkload;
import com.teamcanvas.project.dto.CompletionTrendPoint;
import com.teamcanvas.project.dto.ProjectAnalyticsResponse;
import com.teamcanvas.security.PermissionService;
import com.teamcanvas.task.TaskPriority;
import com.teamcanvas.task.TaskRepository;
import com.teamcanvas.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectAnalyticsService {

    private static final String PROJECT_NOT_FOUND = "Project not found";

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final PermissionService permissionService;

    public ProjectAnalyticsService(ProjectRepository projectRepository,
                                   TaskRepository taskRepository,
                                   PermissionService permissionService) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public ProjectAnalyticsResponse getAnalytics(
            Long projectId, Long userId, LocalDate requestedFrom, LocalDate requestedTo) {
        Project project = projectRepository.findDetailedById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(PROJECT_NOT_FOUND));

        permissionService.requireMember(
                project.getWorkspace().getId(), userId, PROJECT_NOT_FOUND);

        LocalDate to = requestedTo == null ? LocalDate.now() : requestedTo;
        LocalDate from = requestedFrom == null ? to.minusDays(29) : requestedFrom;

        if (from.isAfter(to)) {
            throw new BadRequestException("'from' must be on or before 'to'");
        }

        Map<TaskStatus, Long> statusCounts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            statusCounts.put(status, 0L);
        }
        for (TaskRepository.StatusCount row : taskRepository.countStatusesByProjectId(projectId)) {
            statusCounts.put(row.getStatus(), row.getTaskCount());
        }

        Map<TaskPriority, Long> priorityCounts = new EnumMap<>(TaskPriority.class);
        for (TaskPriority priority : TaskPriority.values()) {
            priorityCounts.put(priority, 0L);
        }
        for (TaskRepository.PriorityCount row : taskRepository.countPrioritiesByProjectId(projectId)) {
            priorityCounts.put(row.getPriority(), row.getTaskCount());
        }

        List<AssigneeWorkload> workload = new ArrayList<>();
        for (TaskRepository.AssigneeTaskCount row : taskRepository.countTasksByAssignee(projectId)) {
            workload.add(new AssigneeWorkload(
                    row.getAssigneeId(), row.getAssigneeName(), row.getTaskCount()));
        }

        long unassignedCount = taskRepository.countByProject_IdAndAssigneeIsNull(projectId);
        if (unassignedCount > 0) {
            workload.add(new AssigneeWorkload(null, "Unassigned", unassignedCount));
        }

        long completed = statusCounts.get(TaskStatus.DONE);
        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        long overdue = taskRepository.countByProject_IdAndStatusNotAndDueDateBefore(
                projectId, TaskStatus.DONE, LocalDate.now());

        Instant fromInclusive = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<LocalDate, Long> completedByDate = new HashMap<>();
        for (TaskRepository.CompletionCount row :
                taskRepository.countCompletionsByDay(projectId, fromInclusive, toExclusive)) {
            completedByDate.put(
                    LocalDate.parse(row.getCompletionDate()),
                    row.getTaskCount());
        }

        List<CompletionTrendPoint> trend = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            trend.add(new CompletionTrendPoint(date, completedByDate.getOrDefault(date, 0L)));
        }

        return new ProjectAnalyticsResponse(
                projectId,
                from,
                to,
                statusCounts,
                priorityCounts,
                completed,
                total - completed,
                overdue,
                workload,
                trend);
    }
}