package com.teamcanvas.comment;

import com.teamcanvas.activity.ActivityLogWriter;
import com.teamcanvas.comment.dto.CommentResponse;
import com.teamcanvas.comment.dto.CreateCommentRequest;
import com.teamcanvas.comment.dto.UpdateCommentRequest;
import com.teamcanvas.common.exception.ConflictException;
import com.teamcanvas.common.exception.ForbiddenException;
import com.teamcanvas.common.exception.ResourceNotFoundException;
import com.teamcanvas.notification.NotificationService;
import com.teamcanvas.project.Project;
import com.teamcanvas.project.ProjectStatus;
import com.teamcanvas.security.PermissionService;
import com.teamcanvas.task.Task;
import com.teamcanvas.task.TaskRepository;
import com.teamcanvas.workspace.WorkspaceMember;
import com.teamcanvas.workspace.WorkspaceRole;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskCommentService {

    private static final String TASK_NOT_FOUND = "Task not found";
    private static final String COMMENT_NOT_FOUND = "Comment not found";

    private final TaskCommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final PermissionService permissionService;
    private final ActivityLogWriter activityLogWriter;
    private final NotificationService notificationService;

    public TaskCommentService(
            TaskCommentRepository commentRepository,
            TaskRepository taskRepository,
            PermissionService permissionService,
            ActivityLogWriter activityLogWriter,
            NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.permissionService = permissionService;
        this.activityLogWriter = activityLogWriter;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long taskId, Long userId) {
        Task task = findTask(taskId);
        permissionService.requireMember(workspaceIdOf(task), userId, TASK_NOT_FOUND);

        return commentRepository.findAllByTask_IdOrderByCreatedAtAscIdAsc(taskId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse create(Long taskId, Long userId, CreateCommentRequest request) {
        Task task = findTask(taskId);
        WorkspaceMember member =
                permissionService.requireMember(workspaceIdOf(task), userId, TASK_NOT_FOUND);
        requireProjectActive(task.getProject());

        TaskComment comment = commentRepository.save(new TaskComment(
                task,
                member.getUser(),
                request.content().trim()));

        activityLogWriter.record(
                workspaceIdOf(task),
                task.getProject().getId(),
                userId,
                ActivityLogWriter.COMMENT_ADDED,
                "COMMENT",
                comment.getId(),
                "taskId=" + task.getId());

        notificationService.commentAdded(task, comment.getId(), userId);

        return CommentResponse.from(comment);
    }

    @Transactional
    public CommentResponse update(Long commentId, Long userId, UpdateCommentRequest request) {
        CommentAccess access = loadCommentForMember(commentId, userId);
        TaskComment comment = access.comment();

        requireProjectActive(comment.getTask().getProject());

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Only the comment author can edit this comment");
        }

        comment.setContent(request.content().trim());
        commentRepository.flush();

        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        CommentAccess access = loadCommentForMember(commentId, userId);
        TaskComment comment = access.comment();

        requireProjectActive(comment.getTask().getProject());

        boolean isAuthor = comment.getAuthor().getId().equals(userId);
        boolean isAdminOrOwner = access.member().getRole() != WorkspaceRole.MEMBER;

        if (!isAuthor && !isAdminOrOwner) {
            throw new ForbiddenException(
                    "Only the comment author or a workspace owner/admin can delete this comment");
        }

        commentRepository.delete(comment);
    }

    private Task findTask(Long taskId) {
        return taskRepository.findDetailedById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(TASK_NOT_FOUND));
    }

    private CommentAccess loadCommentForMember(Long commentId, Long userId) {
        TaskComment comment = commentRepository.findDetailedById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(COMMENT_NOT_FOUND));

        Long workspaceId = workspaceIdOf(comment.getTask());
        WorkspaceMember member =
                permissionService.requireMember(workspaceId, userId, COMMENT_NOT_FOUND);

        return new CommentAccess(comment, member);
    }

    private Long workspaceIdOf(Task task) {
        return task.getProject().getWorkspace().getId();
    }

    private void requireProjectActive(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new ConflictException(
                    "This project is archived; restore it to change its tasks");
        }
    }

    private record CommentAccess(TaskComment comment, WorkspaceMember member) {
    }
}