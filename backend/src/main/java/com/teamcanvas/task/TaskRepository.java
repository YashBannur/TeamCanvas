package com.teamcanvas.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("select t from Task t join fetch t.project join fetch t.reporter left join fetch t.assignee "
            + "where t.id = :id")
    Optional<Task> findDetailedById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"reporter", "assignee"})
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);

    @Query("select coalesce(max(t.sortOrder), -1) from Task t where t.project.id = :projectId and t.status = :status")
    Integer findMaxSortOrder(@Param("projectId") Long projectId, @Param("status") TaskStatus status);

    @Query("select t from Task t where t.project.id = :projectId and t.status = :status "
            + "order by t.sortOrder asc, t.id asc")
    List<Task> findColumn(@Param("projectId") Long projectId, @Param("status") TaskStatus status);

    @Query("select t.project.id as projectId, count(t) as total, "
            + "sum(case when t.status = :doneStatus then 1 else 0 end) as completed "
            + "from Task t where t.project.id in :ids group by t.project.id")
    List<ProjectTaskCounts> countByProjectIds(@Param("ids") Collection<Long> ids,
                                              @Param("doneStatus") TaskStatus doneStatus);

    @Query("select t.status as status, count(t) as taskCount "
            + "from Task t where t.project.id = :projectId group by t.status")
    List<StatusCount> countStatusesByProjectId(@Param("projectId") Long projectId);

    @Query("select t.priority as priority, count(t) as taskCount "
            + "from Task t where t.project.id = :projectId group by t.priority")
    List<PriorityCount> countPrioritiesByProjectId(@Param("projectId") Long projectId);

    @Query("select t.assignee.id as assigneeId, t.assignee.name as assigneeName, "
            + "count(t) as taskCount from Task t "
            + "where t.project.id = :projectId and t.assignee is not null "
            + "group by t.assignee.id, t.assignee.name order by t.assignee.name")
    List<AssigneeTaskCount> countTasksByAssignee(@Param("projectId") Long projectId);

    long countByProject_IdAndAssigneeIsNull(Long projectId);

    long countByProject_IdAndStatusNotAndDueDateBefore(
            Long projectId, TaskStatus status, LocalDate date);

    @Query(value = """
            SELECT DATE_FORMAT(completed_at, '%Y-%m-%d') AS completionDate,
                   COUNT(*) AS taskCount
            FROM tasks
            WHERE project_id = :projectId
              AND status = 'DONE'
              AND completed_at >= :fromInclusive
              AND completed_at < :toExclusive
            GROUP BY DATE_FORMAT(completed_at, '%Y-%m-%d')
            """, nativeQuery = true)
    List<CompletionCount> countCompletionsByDay(
            @Param("projectId") Long projectId,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    interface ProjectTaskCounts {
        Long getProjectId();

        Long getTotal();

        Long getCompleted();
    }

    interface StatusCount {
        TaskStatus getStatus();

        Long getTaskCount();
    }

    interface PriorityCount {
        TaskPriority getPriority();

        Long getTaskCount();
    }

    interface AssigneeTaskCount {
        Long getAssigneeId();

        String getAssigneeName();

        Long getTaskCount();
    }

    interface CompletionCount {
        String getCompletionDate();

        Long getTaskCount();
    }
}