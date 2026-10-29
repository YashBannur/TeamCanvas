package com.teamcanvas.activity;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface ActivityLogRepository extends Repository<ActivityLog, Long> {

    ActivityLog save(ActivityLog activityLog);

    Page<ActivityLog> findByWorkspaceId(Long workspaceId, Pageable pageable);

    Page<ActivityLog> findByProjectId(Long projectId, Pageable pageable);

    Page<ActivityLog> findByProjectIdAndTargetTypeAndTargetId(
            Long projectId,
            String targetType,
            Long targetId,
            Pageable pageable);

    Optional<ActivityLog> findById(Long id);
}