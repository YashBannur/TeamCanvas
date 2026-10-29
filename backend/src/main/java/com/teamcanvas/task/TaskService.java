package com.teamcanvas.task;

import com.teamcanvas.activity.ActivityLogWriter;
import com.teamcanvas.common.exception.BadRequestException;
import com.teamcanvas.common.exception.ConflictException;
import com.teamcanvas.common.exception.ResourceNotFoundException;
import com.teamcanvas.project.Project;
import com.teamcanvas.project.ProjectRepository;
import com.teamcanvas.project.ProjectStatus;
import com.teamcanvas.security.PermissionService;
import com.teamcanvas.task.dto.CreateTaskRequest;
import com.teamcanvas.task.dto.MoveTaskRequest;
import com.teamcanvas.task.dto.TaskResponse;
import com.teamcanvas.task.dto.UpdateTaskRequest;
import com.teamcanvas.user.User;
import com.teamcanvas.workspace.WorkspaceMember;
import com.teamcanvas.workspace.WorkspaceMemberRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private static final String PROJECT_NOT_FOUND = "Project not found";
    private static final String TASK_NOT_FOUND = "Task not found";

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final PermissionService permissionService;
    private final ActivityLogWriter activityLogWriter;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            WorkspaceMemberRepository memberRepository,
            PermissionService permissionService,
            ActivityLogWriter activityLogWriter) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.permissionService = permissionService;
        this.activityLogWriter = activityLogWriter;
    }

    @Transactional
    public TaskResponse create(Long projectId, Long userId, CreateTaskRequest request) {
        Project project = projectRepository.findDetailedById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(PROJECT_NOT_FOUND));
        Long workspaceId = project.getWorkspace().getId();
        WorkspaceMember member =
                permissionService.requireMember(workspaceId, userId, PROJECT_NOT_FOUND);

        requireProjectActive(project);
        validateDates(request.startDate(), request.dueDate());

        User assignee = resolveAssignee(workspaceId, request.assigneeId());
        TaskStatus status = request.status() == null ? TaskStatus.TODO : request.status();

        if (status == TaskStatus.DONE) {
            throw new BadRequestException(
                    "A task must move to In Review before it can be marked Done");
        }

        TaskPriority priority =
                request.priority() == null ? TaskPriority.MEDIUM : request.priority();
        int sortOrder = taskRepository.findMaxSortOrder(projectId, status) + 1;

        Task task = taskRepository.save(new Task(
                project,
                request.title().trim(),
                blankToNull(request.description()),
                status,
                priority,
                assignee,
                member.getUser(),
                request.startDate(),
                request.dueDate(),
                sortOrder));

        activityLogWriter.record(
                workspaceId,
                projectId,
                userId,
                ActivityLogWriter.TASK_CREATED,
                "TASK",
                task.getId(),
                "title: " + task.getTitle() + "; status: " + task.getStatus());

        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getOne(Long taskId, Long userId) {
        return TaskResponse.from(loadForMember(taskId, userId).task());
    }

    @Transactional
    public TaskResponse update(
            Long taskId, Long userId, UpdateTaskRequest request) {
        TaskAccess access = loadForMember(taskId, userId);
        Task task = access.task();

        requireProjectActive(task.getProject());
        permissionService.requireCanEditTask(access.member(), task);
        checkVersion(task, request.version());

        boolean unassign = Boolean.TRUE.equals(request.unassign());
        if (unassign && request.assigneeId() != null) {
            throw new BadRequestException(
                    "Send either assigneeId or unassign, not both");
        }

        List<String> generalChanges = new ArrayList<>();

        if (request.title() != null) {
            String oldTitle = task.getTitle();
            String newTitle = request.title().trim();
            if (!Objects.equals(oldTitle, newTitle)) {
                task.setTitle(newTitle);
                generalChanges.add(
                        "title: " + oldTitle + " -> " + newTitle);
            }
        }

        if (request.description() != null) {
            String oldDescription = task.getDescription();
            String newDescription = blankToNull(request.description());
            if (!Objects.equals(oldDescription, newDescription)) {
                task.setDescription(newDescription);
                generalChanges.add("description changed");
            }
        }

        if (request.priority() != null
                && task.getPriority() != request.priority()) {
            TaskPriority oldPriority = task.getPriority();
            task.setPriority(request.priority());

            activityLogWriter.record(
                    workspaceIdOf(task),
                    task.getProject().getId(),
                    userId,
                    ActivityLogWriter.TASK_PRIORITY_CHANGED,
                    "TASK",
                    task.getId(),
                    "priority: " + oldPriority + " -> " + task.getPriority());
        }

        if (unassign) {
            if (task.getAssignee() != null) {
                String oldAssignee = task.getAssignee().getName();
                task.setAssignee(null);

                activityLogWriter.record(
                        workspaceIdOf(task),
                        task.getProject().getId(),
                        userId,
                        ActivityLogWriter.TASK_ASSIGNED,
                        "TASK",
                        task.getId(),
                        "assignee: " + oldAssignee + " -> Unassigned");
            }
        } else if (request.assigneeId() != null) {
            User newAssignee =
                    resolveAssignee(workspaceIdOf(task), request.assigneeId());
            Long oldAssigneeId =
                    task.getAssignee() == null ? null : task.getAssignee().getId();

            if (!Objects.equals(oldAssigneeId, newAssignee.getId())) {
                String oldAssignee =
                        task.getAssignee() == null
                                ? "Unassigned"
                                : task.getAssignee().getName();
                task.setAssignee(newAssignee);

                activityLogWriter.record(
                        workspaceIdOf(task),
                        task.getProject().getId(),
                        userId,
                        ActivityLogWriter.TASK_ASSIGNED,
                        "TASK",
                        task.getId(),
                        "assignee: " + oldAssignee + " -> " + newAssignee.getName());
            }
        }

        if (request.startDate() != null
                && !Objects.equals(task.getStartDate(), request.startDate())) {
            LocalDate oldStartDate = task.getStartDate();
            task.setStartDate(request.startDate());
            generalChanges.add(
                    "start date: " + oldStartDate + " -> " + task.getStartDate());
        }

        if (request.dueDate() != null
                && !Objects.equals(task.getDueDate(), request.dueDate())) {
            LocalDate oldDueDate = task.getDueDate();
            task.setDueDate(request.dueDate());

            activityLogWriter.record(
                    workspaceIdOf(task),
                    task.getProject().getId(),
                    userId,
                    ActivityLogWriter.TASK_DUE_DATE_CHANGED,
                    "TASK",
                    task.getId(),
                    "due date: " + oldDueDate + " -> " + task.getDueDate());
        }

        validateDates(task.getStartDate(), task.getDueDate());
        taskRepository.flush();

        if (!generalChanges.isEmpty()) {
            activityLogWriter.record(
                    workspaceIdOf(task),
                    task.getProject().getId(),
                    userId,
                    ActivityLogWriter.TASK_UPDATED,
                    "TASK",
                    task.getId(),
                    String.join("; ", generalChanges));
        }

        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse move(
            Long taskId, Long userId, MoveTaskRequest request) {
        TaskAccess access = loadForMember(taskId, userId);
        Task task = access.task();

        requireProjectActive(task.getProject());
        permissionService.requireCanMoveTask(
                access.member(), task, request.status());
        checkVersion(task, request.version());

        TaskStatus oldStatus = task.getStatus();

        List<Task> column = new ArrayList<>(
                taskRepository.findColumn(
                        task.getProject().getId(), request.status()));
        column.removeIf(other -> other.getId().equals(task.getId()));

        int insertAt = 0;
        if (request.afterTaskId() != null) {
            int afterIndex = -1;
            for (int i = 0; i < column.size(); i++) {
                if (column.get(i).getId().equals(request.afterTaskId())) {
                    afterIndex = i;
                    break;
                }
            }
            if (afterIndex < 0) {
                throw new BadRequestException(
                        "afterTaskId must be a different task in the target column");
            }
            insertAt = afterIndex + 1;
        }

        task.setStatus(request.status());
        column.add(insertAt, task);
        for (int i = 0; i < column.size(); i++) {
            column.get(i).setSortOrder(i);
        }

        taskRepository.flush();

        if (oldStatus != request.status()) {
            boolean approved =
                    oldStatus == TaskStatus.IN_REVIEW
                            && request.status() == TaskStatus.DONE;

            activityLogWriter.record(
                    workspaceIdOf(task),
                    task.getProject().getId(),
                    userId,
                    approved
                            ? ActivityLogWriter.TASK_APPROVED
                            : ActivityLogWriter.TASK_STATUS_CHANGED,
                    "TASK",
                    task.getId(),
                    "status: " + oldStatus + " -> " + request.status());
        }

        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long taskId, Long userId) {
        TaskAccess access = loadForMember(taskId, userId);
        Task task = access.task();

        requireProjectActive(task.getProject());
        permissionService.requireCanDeleteTask(access.member(), task);

        activityLogWriter.record(
                workspaceIdOf(task),
                task.getProject().getId(),
                userId,
                ActivityLogWriter.TASK_DELETED,
                "TASK",
                task.getId(),
                "title: " + task.getTitle() + "; status: " + task.getStatus());

        taskRepository.delete(task);
    }

    private record TaskAccess(Task task, WorkspaceMember member) {
    }

    private TaskAccess loadForMember(Long taskId, Long userId) {
        Task task = taskRepository.findDetailedById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(TASK_NOT_FOUND));
        WorkspaceMember member = permissionService.requireMember(
                workspaceIdOf(task), userId, TASK_NOT_FOUND);
        return new TaskAccess(task, member);
    }

    private Long workspaceIdOf(Task task) {
        return task.getProject().getWorkspace().getId();
    }

    private User resolveAssignee(Long workspaceId, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, assigneeId)
                .map(WorkspaceMember::getUser)
                .orElseThrow(() -> new BadRequestException(
                        "Assignee must be a member of this workspace"));
    }

    private void requireProjectActive(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new ConflictException(
                    "This project is archived; restore it to change its tasks");
        }
    }

    private void checkVersion(Task task, Long clientVersion) {
        if (clientVersion != null
                && clientVersion.longValue() != task.getVersion()) {
            throw new ConflictException(
                    "This task was changed by someone else. Reload it and try again");
        }
    }

    private void validateDates(LocalDate start, LocalDate due) {
        if (start != null && due != null && due.isBefore(start)) {
            throw new BadRequestException(
                    "Due date cannot be before start date");
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