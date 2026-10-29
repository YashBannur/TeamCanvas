package com.teamcanvas.project.dto;

public record AssigneeWorkload(
        Long assigneeId,
        String assigneeName,
        long taskCount) {
}