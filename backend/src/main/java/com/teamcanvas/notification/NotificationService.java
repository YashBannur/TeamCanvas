package com.teamcanvas.notification;

import com.teamcanvas.common.dto.PageResponse;
import com.teamcanvas.common.exception.ResourceNotFoundException;
import com.teamcanvas.invitation.WorkspaceInvitation;
import com.teamcanvas.project.Project;
import com.teamcanvas.security.PermissionService;
import com.teamcanvas.task.Task;
import com.teamcanvas.workspace.WorkspaceMember;
import com.teamcanvas.workspace.WorkspaceMemberRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final PermissionService permissionService;

    public NotificationService(NotificationRepository notificationRepository,
                               WorkspaceMemberRepository memberRepository,
                               PermissionService permissionService) {
        this.notificationRepository = notificationRepository;
        this.memberRepository = memberRepository;
        this.permissionService = permissionService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void taskAssigned(Task task, Long actorId) {
        if (task.getAssignee() == null) {
            return;
        }

        Long recipientId = task.getAssignee().getId();
        if (recipientId.equals(actorId)) {
            return;
        }

        String dedupeKey = "TASK_ASSIGNED:" + task.getId()
                + ":" + task.getVersion() + ":" + recipientId;

        saveIfAbsent(new Notification(
                recipientId,
                NotificationType.TASK_ASSIGNED,
                "You were assigned to task: " + task.getTitle(),
                "TASK",
                task.getId(),
                dedupeKey));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void commentAdded(Task task, Long commentId, Long actorId) {
        Set<Long> recipients = new LinkedHashSet<>();

        if (task.getAssignee() != null) {
            recipients.add(task.getAssignee().getId());
        }
        recipients.add(task.getReporter().getId());
        recipients.remove(actorId);

        for (Long recipientId : recipients) {
            String dedupeKey = "COMMENT_ADDED:" + commentId + ":" + recipientId;

            saveIfAbsent(new Notification(
                    recipientId,
                    NotificationType.COMMENT_ADDED,
                    "New comment on task: " + task.getTitle(),
                    "TASK",
                    task.getId(),
                    dedupeKey));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void invitationReceived(WorkspaceInvitation invitation) {
        Long recipientId = invitation.getInvitee().getId();

        saveIfAbsent(new Notification(
                recipientId,
                NotificationType.INVITATION_RECEIVED,
                "You were invited to join workspace: " + invitation.getWorkspace().getName(),
                "INVITATION",
                invitation.getId(),
                "INVITATION_RECEIVED:" + invitation.getId()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void projectArchived(Project project, Long actorId) {
        Long workspaceId = project.getWorkspace().getId();

        // updatedAt is flushed by ProjectService before this method is called.
        String eventKey = project.getUpdatedAt().toString();

        List<WorkspaceMember> members =
                memberRepository.findAllByWorkspaceIdWithUser(workspaceId);

        for (WorkspaceMember member : members) {
            Long recipientId = member.getUser().getId();
            if (recipientId.equals(actorId)) {
                continue;
            }

            saveIfAbsent(new Notification(
                    recipientId,
                    NotificationType.PROJECT_ARCHIVED,
                    "Project archived: " + project.getName(),
                    "PROJECT",
                    project.getId(),
                    "PROJECT_ARCHIVED:" + project.getId() + ":" + eventKey + ":" + recipientId));
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(
            Long userId, int page, int size, boolean unreadOnly) {
        PageRequest pageRequest = pageRequest(page, size);

        Page<Notification> notifications = unreadOnly
                ? notificationRepository.findByRecipientIdAndIsReadFalse(userId, pageRequest)
                : notificationRepository.findByRecipientId(userId, pageRequest);

        return PageResponse.from(notifications.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(
                notificationRepository.countByRecipientIdAndIsReadFalse(userId));
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.markRead();
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    private void saveIfAbsent(Notification notification) {
        if (!notificationRepository.existsByDedupeKey(notification.getDedupeKey())) {
            notificationRepository.save(notification);
        }
    }

    private PageRequest pageRequest(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id"));
        return PageRequest.of(safePage, safeSize, sort);
    }
}