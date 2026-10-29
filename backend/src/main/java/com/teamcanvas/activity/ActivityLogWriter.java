package com.teamcanvas.activity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogWriter {

    public static final String PROJECT_CREATED = "PROJECT_CREATED";
    public static final String PROJECT_ARCHIVED = "PROJECT_ARCHIVED";
    public static final String PROJECT_DELETED = "PROJECT_DELETED";

    public static final String TASK_CREATED = "TASK_CREATED";
    public static final String TASK_UPDATED = "TASK_UPDATED";
    public static final String TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String TASK_STATUS_CHANGED = "TASK_STATUS_CHANGED";
    public static final String TASK_APPROVED = "TASK_APPROVED";
    public static final String TASK_PRIORITY_CHANGED = "TASK_PRIORITY_CHANGED";
    public static final String TASK_DUE_DATE_CHANGED = "TASK_DUE_DATE_CHANGED";
    public static final String TASK_DELETED = "TASK_DELETED";

    public static final String COMMENT_ADDED = "COMMENT_ADDED";

    public static final String MEMBER_ADDED = "MEMBER_ADDED";
    public static final String MEMBER_REMOVED = "MEMBER_REMOVED";
    public static final String MEMBER_ROLE_CHANGED = "MEMBER_ROLE_CHANGED";

    public static final String INVITATION_SENT = "INVITATION_SENT";

    private final ActivityLogRepository repository;

    public ActivityLogWriter(ActivityLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            Long workspaceId,
            Long projectId,
            Long actorId,
            String action,
            String targetType,
            Long targetId,
            String details) {
        repository.save(new ActivityLog(
                workspaceId,
                projectId,
                actorId,
                action,
                targetType,
                targetId,
                details));
    }
}