package com.teamcanvas.comment.dto;

import com.teamcanvas.comment.TaskComment;
import java.time.Instant;

public record CommentResponse(
        Long id,
        Long taskId,
        Long authorId,
        String content,
        Instant createdAt,
        Instant updatedAt) {

    public static CommentResponse from(TaskComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                comment.getAuthor().getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}