package com.teamcanvas.dashboard;

import com.teamcanvas.project.Project;
import com.teamcanvas.project.ProjectStatus;
import com.teamcanvas.task.Task;
import com.teamcanvas.task.TaskStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * The dashboard's queries. They span several entities, so they live together in one class (JPQL through the
 * EntityManager) instead of being spread over the entity repositories.
 * Every query except the first is restricted to the workspace ids found by the first one: the workspaces the
 * user belongs to. That is what keeps one user's dashboard away from another user's data.
 */
@Repository
public class DashboardRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public record TaskCounts(long open, long dueSoon, long overdue) {
    }

    /** The security boundary: the workspaces this user is a member of. */
    public List<Long> findWorkspaceIds(Long userId) {
        return entityManager.createQuery(
                        "select m.workspace.id from WorkspaceMember m where m.user.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public long countActiveProjects(List<Long> workspaceIds) {
        return entityManager.createQuery(
                        "select count(p) from Project p "
                                + "where p.workspace.id in :workspaceIds and p.status = :active", Long.class)
                .setParameter("workspaceIds", workspaceIds)
                .setParameter("active", ProjectStatus.ACTIVE)
                .getSingleResult();
    }

    /** Open / due soon / overdue in ONE query (conditional aggregation). */
    public TaskCounts countMyOpenTasks(Long userId, List<Long> workspaceIds, LocalDate today, LocalDate soonEnd) {
        Object[] row = entityManager.createQuery(
                        "select count(t), "
                                + "sum(case when t.dueDate >= :today and t.dueDate <= :soonEnd then 1 else 0 end), "
                                + "sum(case when t.dueDate < :today then 1 else 0 end) "
                                + "from Task t "
                                + "where t.assignee.id = :userId and t.status <> :done "
                                + "and t.project.status = :active and t.project.workspace.id in :workspaceIds",
                        Object[].class)
                .setParameter("today", today)
                .setParameter("soonEnd", soonEnd)
                .setParameter("userId", userId)
                .setParameter("done", TaskStatus.DONE)
                .setParameter("active", ProjectStatus.ACTIVE)
                .setParameter("workspaceIds", workspaceIds)
                .getSingleResult();
        return new TaskCounts(toLong(row[0]), toLong(row[1]), toLong(row[2]));
    }

    /** My open tasks, soonest due date first; tasks without a due date come last. */
    public List<Task> findMyOpenTasks(Long userId, List<Long> workspaceIds, int limit) {
        return entityManager.createQuery(
                        "select t from Task t "
                                + "join fetch t.project p join fetch p.workspace join fetch t.assignee "
                                + "where t.assignee.id = :userId and t.status <> :done "
                                + "and p.status = :active and p.workspace.id in :workspaceIds "
                                + "order by case when t.dueDate is null then 1 else 0 end, t.dueDate asc, t.id asc",
                        Task.class)
                .setParameter("userId", userId)
                .setParameter("done", TaskStatus.DONE)
                .setParameter("active", ProjectStatus.ACTIVE)
                .setParameter("workspaceIds", workspaceIds)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Project> findRecentActiveProjects(List<Long> workspaceIds, int limit) {
        return entityManager.createQuery(
                        "select p from Project p join fetch p.createdBy "
                                + "where p.workspace.id in :workspaceIds and p.status = :active "
                                + "order by p.updatedAt desc, p.id desc", Project.class)
                .setParameter("workspaceIds", workspaceIds)
                .setParameter("active", ProjectStatus.ACTIVE)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Task> findRecentTasks(List<Long> workspaceIds, int limit) {
        return entityManager.createQuery(
                        "select t from Task t "
                                + "join fetch t.project p join fetch p.workspace left join fetch t.assignee "
                                + "where p.workspace.id in :workspaceIds "
                                + "order by t.updatedAt desc, t.id desc", Task.class)
                .setParameter("workspaceIds", workspaceIds)
                .setMaxResults(limit)
                .getResultList();
    }

    /** SUM over zero rows is NULL in SQL; treat it as 0. */
    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}