package com.teamcanvas.task;

import com.teamcanvas.common.dto.PageResponse;
import com.teamcanvas.common.exception.BadRequestException;
import com.teamcanvas.common.exception.ResourceNotFoundException;
import com.teamcanvas.project.Project;
import com.teamcanvas.project.ProjectRepository;
import com.teamcanvas.security.PermissionService;
import com.teamcanvas.task.dto.BoardResponse;
import com.teamcanvas.task.dto.TaskResponse;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of tasks: search, filter, sort, paginate, and the Kanban board. Writes live in TaskService. */
@Service
public class TaskQueryService {

    static final int MAX_PAGE_SIZE = 100;
    static final int BOARD_LIMIT = 500;

    private static final String PROJECT_NOT_FOUND = "Project not found";
    private static final String ALLOWED_SORT_FIELDS = "title, dueDate, createdAt, updatedAt, position";

    /** What the client may sort by (API name -> entity field). Anything else is rejected. */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "title", "title",
            "dueDate", "dueDate",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "position", "sortOrder");

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final PermissionService permissionService;

    public TaskQueryService(TaskRepository taskRepository,
                            ProjectRepository projectRepository,
                            PermissionService permissionService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> search(Long projectId, Long userId, TaskFilter filter,
                                             int page, int size, String sortBy, String direction) {
        requireProjectMember(projectId, userId);
        Specification<Task> specification = buildSpecification(projectId, filter);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                buildSort(sortBy, direction));

        Page<Task> result = taskRepository.findAll(specification, pageable);
        return PageResponse.from(result.map(TaskResponse::from));
    }

    /** All matching tasks grouped by column, each column ordered by position (up to BOARD_LIMIT tasks). */
    @Transactional(readOnly = true)
    public BoardResponse board(Long projectId, Long userId, TaskFilter filter) {
        requireProjectMember(projectId, userId);
        Specification<Task> specification = buildSpecification(projectId, filter);
        Pageable pageable = PageRequest.of(0, BOARD_LIMIT,
                Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.ASC, "id")));

        Page<Task> result = taskRepository.findAll(specification, pageable);

        Map<TaskStatus, List<TaskResponse>> columns = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            columns.put(status, new ArrayList<>());
        }
        for (Task task : result.getContent()) {
            columns.get(task.getStatus()).add(TaskResponse.from(task));
        }
        return new BoardResponse(columns, result.hasNext());
    }

    // ---------------------------------------------------------------- helpers

    /** A missing project and somebody else's project both give the same 404. */
    private void requireProjectMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(PROJECT_NOT_FOUND));
        permissionService.requireMember(project.getWorkspace().getId(), userId, PROJECT_NOT_FOUND);
    }

    private Specification<Task> buildSpecification(Long projectId, TaskFilter filter) {
        if (Boolean.TRUE.equals(filter.unassigned()) && filter.assigneeId() != null) {
            throw new BadRequestException("Use either assigneeId or unassigned, not both");
        }
        if (filter.dueFrom() != null && filter.dueTo() != null && filter.dueTo().isBefore(filter.dueFrom())) {
            throw new BadRequestException("dueTo cannot be before dueFrom");
        }

        // Always restricted to this project, whatever else the caller sends.
        Specification<Task> specification = TaskSpecifications.inProject(projectId);
        if (filter.q() != null && !filter.q().isBlank()) {
            specification = specification.and(TaskSpecifications.titleContains(filter.q()));
        }
        if (filter.status() != null) {
            specification = specification.and(TaskSpecifications.hasStatus(filter.status()));
        }
        if (filter.priority() != null) {
            specification = specification.and(TaskSpecifications.hasPriority(filter.priority()));
        }
        if (filter.assigneeId() != null) {
            specification = specification.and(TaskSpecifications.assignedTo(filter.assigneeId()));
        }
        if (Boolean.TRUE.equals(filter.unassigned())) {
            specification = specification.and(TaskSpecifications.unassigned());
        }
        if (filter.dueFrom() != null) {
            specification = specification.and(TaskSpecifications.dueOnOrAfter(filter.dueFrom()));
        }
        if (filter.dueTo() != null) {
            specification = specification.and(TaskSpecifications.dueOnOrBefore(filter.dueTo()));
        }
        return specification;
    }

    /** Whitelisted field + direction, with "id" as a tie-breaker so pages never repeat or skip rows. */
    private Sort buildSort(String sortBy, String direction) {
        String field = SORT_FIELDS.get(sortBy);
        if (field == null) {
            throw new BadRequestException("Unsupported sortBy. Allowed: " + ALLOWED_SORT_FIELDS);
        }
        Sort.Direction sortDirection = Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new BadRequestException("direction must be asc or desc"));
        return Sort.by(sortDirection, field).and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
