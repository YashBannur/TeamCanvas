package com.teamcanvas.task;

import java.time.LocalDate;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Small building blocks for the dynamic task query. The service combines only the ones the caller asked for.
 * Everything is bound as a parameter, so user input can never change the structure of the SQL.
 */
public final class TaskSpecifications {

    /** '!' is the escape character for LIKE patterns (a backslash is awkward in MySQL string literals). */
    private static final char LIKE_ESCAPE = '!';

    private TaskSpecifications() {
    }

    public static Specification<Task> inProject(Long projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    /** Case-insensitive "title contains text". The % and _ typed by the user are treated as plain characters. */
    public static Specification<Task> titleContains(String text) {
        String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> assignedTo(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), userId);
    }

    public static Specification<Task> unassigned() {
        return (root, query, cb) -> cb.isNull(root.get("assignee"));
    }

    public static Specification<Task> dueOnOrAfter(LocalDate date) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("dueDate"), date);
    }

    public static Specification<Task> dueOnOrBefore(LocalDate date) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("dueDate"), date);
    }

    private static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
