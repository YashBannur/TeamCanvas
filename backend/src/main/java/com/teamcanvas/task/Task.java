package com.teamcanvas.task;

import com.teamcanvas.project.Project;
import com.teamcanvas.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false, updatable = false)
    private User reporter;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OptimisticLock(excluded = true)
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean contentChanged;

    protected Task() {
    }

    public Task(Project project, String title, String description, TaskStatus status, TaskPriority priority,
                User assignee, User reporter, LocalDate startDate, LocalDate dueDate, int sortOrder) {
        this.project = project;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assignee = assignee;
        this.reporter = reporter;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (status == TaskStatus.DONE && completedAt == null) {
            completedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        if (contentChanged) {
            updatedAt = Instant.now();
        }
    }

    @PostPersist
    @PostUpdate
    void resetContentChanged() {
        contentChanged = false;
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (!Objects.equals(this.title, title)) {
            this.title = title;
            contentChanged = true;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (!Objects.equals(this.description, description)) {
            this.description = description;
            contentChanged = true;
        }
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        if (this.status != status) {
            this.status = status;
            this.completedAt = status == TaskStatus.DONE ? Instant.now() : null;
            contentChanged = true;
        }
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        if (this.priority != priority) {
            this.priority = priority;
            contentChanged = true;
        }
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        Long currentId = this.assignee == null ? null : this.assignee.getId();
        Long newId = assignee == null ? null : assignee.getId();
        if (!Objects.equals(currentId, newId)) {
            this.assignee = assignee;
            contentChanged = true;
        }
    }

    public User getReporter() {
        return reporter;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        if (!Objects.equals(this.startDate, startDate)) {
            this.startDate = startDate;
            contentChanged = true;
        }
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        if (!Objects.equals(this.dueDate, dueDate)) {
            this.dueDate = dueDate;
            contentChanged = true;
        }
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}